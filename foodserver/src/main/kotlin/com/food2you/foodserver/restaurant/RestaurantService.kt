package com.food2you.foodserver.restaurant

import com.food2you.foodserver.combo.Combo
import com.food2you.foodserver.costumer.Costumer
import com.food2you.foodserver.menus.Menu
import com.food2you.foodserver.menus.MenuRepository
import com.food2you.foodserver.menus.requests.NewMenu
import com.food2you.foodserver.orders.Order
import com.food2you.foodserver.orders.OrderRepository
import com.food2you.foodserver.product.Product
import com.food2you.foodserver.product.ProductRepository
import com.food2you.foodserver.restaurant.requests.NewRestaurant
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
data class RestaurantService (
    val restaurantRepository: RestaurantRepository,
    val orderRepository: OrderRepository,
    val menuRepository: MenuRepository,
    val productRepository: ProductRepository
) {
    private val logger = LoggerFactory.getLogger(Costumer::class.java)

    fun createRestaurant(restaurant: NewRestaurant) {
        val restaurant = Restaurant(
            id = null,
            name = restaurant.name,
            status = "Open",
            orders = mutableListOf<Order>(),
            menus = mutableListOf<Menu>(),
            products = mutableListOf<Product>(),
            combos = mutableListOf<Combo>()
        )

        restaurantRepository.save(restaurant)
    }



    fun findAllRestaurants() = restaurantRepository.findAll()

    fun getAllOrders(restaurantId: Long) : MutableList<Order> = orderRepository.findAllByRestaurant(restaurantId)

    fun createMenu(newMenu: NewMenu, restaurantId: Long) : Menu {
        val menu = Menu (
            id = null,
            name = newMenu.name,
            restaurant = restaurantId,
            products = mutableSetOf<Product>()
        )

        menu.restaurant = restaurantId
        return menuRepository.save(menu)
    }

    fun createMenu(menu: Menu, restaurantId: Long) : Menu {
        menu.restaurant = restaurantId
        return menuRepository.save(menu)
    }

    fun getAllMenus(restaurantId: Long) : MutableList<Menu> = menuRepository.findAllByRestaurant(restaurantId)

    fun addProductToMenu(productId : Long, menuId: Long ) {
        val menu = menuRepository.findByIdOrNull(menuId)
        val product = productRepository.findByIdOrNull(productId)
        menu?.products?.add(product!!)
        menuRepository.deleteById(menuId)
        createMenu(menu!!, menuId)
    }

    fun addProduct(product : Product, restaurantId: Long) : Product {
        product.restaurant = restaurantId
        return productRepository.save(product)
    }

    fun getAllProducts(restaurantId: Long) : MutableList<Product> = productRepository.findAllByRestaurant(restaurantId)

    fun getAllProductsFromOrder(orderId : Long) : MutableList<Product> = orderRepository.findById(orderId).get().products

    fun deleteProduct(productId: Long) = productRepository.deleteById(productId)

    fun getProductById(productId: Long) : Product? = productRepository.findByIdOrNull(productId)

    fun updateProduct(productId: Long, product: Product) {
        var deletedProduct = getProductById(productId)
        deleteProduct(productId)
        if (deletedProduct != null) {
            addProduct(product, deletedProduct.restaurant)
        } else {
            println("PRODUCT NOT FOUND")
        }
    }
}
