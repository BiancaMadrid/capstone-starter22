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

        logger.error("Failed to clear shopping cart for user{}", userId, e);
            throw new RuntimeException("Error clearing shopping cart for user ID " + userId, e);
   }
 }
}