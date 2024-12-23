package org.yearup.data;

import org.yearup.models.ShoppingCart;

public interface ShoppingCartDao
{
public interface ShoppingCartDao {
    ShoppingCart getByUserId(int userId);
    // add additional method signatures here
}

    // Add additional method signatures here (without body)
    void saveOrUpdate(ShoppingCart shoppingCart);

    void clearCart(int userId);

    void removeItem(int userId, int productId);

    void addProductToCart(int userId, int productId);

    void updateProductQuantity(int userId, int productId, int quantity);
}