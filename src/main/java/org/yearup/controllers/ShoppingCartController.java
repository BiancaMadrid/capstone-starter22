package org.yearup.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;
import org.yearup.models.User;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

// convert this class to a REST controller
// only logged in users should have access to these actions
public class ShoppingCartController
{
    // a shopping cart requires
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;
    private ProductDao productDao;
// only logged-in users should have access to these actions
@RestController
@RequestMapping("/cart")
@PreAuthorize("isAuthenticated()")
public class ShoppingCartController {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    private final ShoppingCartDao shoppingCartDao;
    private final UserDao userDao;
    private final ProductDao productDao;

    @Autowired
    public ShoppingCartController(ShoppingCartDao shoppingCartDao, UserDao userDao, ProductDao productDao) {
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
        this.productDao = productDao;
    }


    // each method in this controller requires a Principal object as a parameter
    public ShoppingCart getCart(Principal principal)
    {
        try
        {
            // get the currently logged in username
    @GetMapping
    public ShoppingCart getCart(Principal principal) {
        try {
            String userName = principal.getName();
            // find database user by userId
            logger.info("Fetching cart for user: {}", userName);

            User user = userDao.getByUserName(userName);
            int userId = user.getId();
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            // use the shoppingcartDao to get all items in the cart and return the cart
            return null;
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
            ShoppingCart cart = shoppingCartDao.getByUserId(user.getId());

            logger.info("Cart retrieved successfully for user: {}", userName);
            return cart;
        } catch (Exception e) {
            logger.error("Error fetching cart", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve shopping cart.", e);
        }
    }

    // add a POST method to add a product to the cart - the url should be
    // https://localhost:8080/cart/products/15 (15 is the productId to be added
    @PostMapping("/products/{productId}")
    public ResponseEntity<Void> addProductToCart(@PathVariable int productId, Principal principal) {
        try {
            if (productId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product ID");
            }

            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            if (productDao.getById(productId) == null) { // Fixed here
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }

            shoppingCartDao.addProductToCart(user.getId(), productId);
            logger.info("Product {} added to cart for user: {}", productId, userName);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Error adding product to cart", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to add product to cart.", e);
        }
    }


    // add a PUT method to update an existing product in the cart - the url should be
    // https://localhost:8080/cart/products/15 (15 is the productId to be updated)
    // the BODY should be a ShoppingCartItem - quantity is the only value that will be updated
    @PutMapping("/products/{productId}")
    public ResponseEntity<Void> updateCartItem(@PathVariable int productId, @RequestBody ShoppingCartItem item, Principal principal) {
        try {
            if (productId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product ID");
            }

            if (item.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
            }

            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            shoppingCartDao.updateProductQuantity(user.getId(), productId, item.getQuantity());
            logger.info("Updated product {} to quantity {} for user: {}", productId, item.getQuantity(), userName);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error updating product in cart", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update product in cart.", e);
        }
    }


    // add a DELETE method to clear all products from the current users cart
    // https://localhost:8080/cart

}
