package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao {
    private static final Logger logger = LoggerFactory.getLogger(MySqlShoppingCartDao.class); // Logger for error handling

    @Autowired
    public MySqlShoppingCartDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ShoppingCart getByUserId(int userId) {
        String query = "SELECT sc.product_id, sc.quantity, p.name, p.price FROM shopping_cart sc " +
                "JOIN products p ON sc.product_id = p.product_id WHERE sc.user_id = ?";
        ShoppingCart cart = new ShoppingCart();
        Map<Integer, ShoppingCartItem> items = new HashMap<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int productId = resultSet.getInt("product_id");
                    String name = resultSet.getString("name");
                    BigDecimal price = resultSet.getBigDecimal("price");
                    int quantity = resultSet.getInt("quantity");

                    Product product = new Product();
                    product.setProductId(productId);
                    product.setName(name);
                    product.setPrice(price);

                    ShoppingCartItem item = new ShoppingCartItem();
                    item.setProduct(product);
                    item.setQuantity(quantity);

                    items.put(productId, item);
                }
                cart.setItems(items);

            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve shopping cart for user ID {}", userId, e);
            throw new RuntimeException("Error retrieving shopping cart for user ID " + userId, e);
        }

        return cart;
    }

    @Override
    @Transactional
    public void saveOrUpdate(ShoppingCart shoppingCart) {
        if (shoppingCart == null || shoppingCart.getItems() == null) {
            throw new IllegalArgumentException("Could not save or update shopping cart.");
        }

        if (shoppingCart.getItems().isEmpty()) {
            logger.warn("Shopping cart for user ID {} contains no items to save.", shoppingCart.user.getUserId());
            return; // Nothing to save
        }

        String deleteQuery = "DELETE FROM shopping_cart WHERE user_id = ?";
        String insertQuery = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?)";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            // Deleting old items for the user
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                deleteStatement.setInt(1, shoppingCart.user.getUserId());
                deleteStatement.executeUpdate();
            }

            // Inserting updated items
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                for (ShoppingCartItem item : shoppingCart.getItems().values()) {
                    insertStatement.setInt(1, shoppingCart.user.getUserId());
                    insertStatement.setInt(2, item.getProduct().getProductId());
                    insertStatement.setInt(3, item.getQuantity());
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            logger.error("Failed to save or update shopping cart for user ID {}", shoppingCart.user.getUserId(), e);
            throw new RuntimeException("Error saving or updating shopping cart for user ID " + shoppingCart.user.getUserId(), e);
        }
    }

    @Override
    public void addProductToCart(int userId, int productId) {
        String selectQuery = "SELECT quantity FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        String insertQuery = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?)";
        String updateQuery = "UPDATE shopping_cart SET quantity = quantity + 1 WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection()) {
            // Check if the product is already in the cart
            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                selectStatement.setInt(1, userId);
                selectStatement.setInt(2, productId);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (resultSet.next()) {
                        // Update quantity if product exists in cart
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                            updateStatement.setInt(1, userId);
                            updateStatement.setInt(2, productId);
                            updateStatement.executeUpdate();
                        }
                    } else {
                        // Insert a new row if product does not exist
                        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                            insertStatement.setInt(1, userId);
                            insertStatement.setInt(2, productId);
                            insertStatement.setInt(3, 1);
                            insertStatement.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to add product {} to shopping cart for user {}", productId, userId, e);
            throw new RuntimeException("Error adding product to shopping cart.", e);
        }
    }

    @Override
    @Transactional
    public void updateProductQuantity(int userId, int productId, int quantity) {
        if (quantity <= 0) {
            removeItem(userId, productId); // Remove item if quantity is 0 or less
            return;
        }

        String updateQuery = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {
            statement.setInt(1, quantity);
            statement.setInt(2, userId);
            statement.setInt(3, productId);
            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                logger.warn("No product with ID {} in the shopping cart for user ID {}.", productId, userId);
            }
        } catch (SQLException e) {
            logger.error("Failed to update quantity of product {} for user {}", productId, userId, e);
            throw new RuntimeException("Error updating product quantity in shopping cart.", e);
        }
    }

    @Override
    public void removeItem(int userId, int productId) {
        String query = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, productId);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                logger.warn("No item found with product ID {} for user ID {}. Nothing was removed.", productId, userId);
            }
        } catch (SQLException e) {
            logger.error("Could not remove product {} from user {} cart", productId, userId, e);
            throw new RuntimeException("Error removing product ID " + productId + " from shopping cart for user ID " + userId, e);
        }
    }

    @Override
    @Transactional
    public void clearCart(int userId) {
        String query = "DELETE FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                logger.warn("Shopping cart was already empty for user ID {}", userId);
            }
        } catch (SQLException e) {
        logger.error("Failed to clear shopping cart for user{}", userId, e);
            throw new RuntimeException("Error clearing shopping cart for user ID " + userId, e);
   }
 }
}