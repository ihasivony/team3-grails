package test

import grails.gorm.services.Service
import grails.gorm.transactions.Transactional


interface IUserService {

    User get(Serializable id)

    List<User> list(Map args)

    Long count()

    void delete(Serializable id)

    User save(User user)



}
@Service(User)
@Transactional
abstract class UserService implements IUserService {
    def addItemToCart(Long idUser, Long idProduct, Integer quantity){
        //We get the user first
        User user = User.get(idUser)
        //Then we get his cart
        Cart cart = user.cart
        //Get the product
        Product product = Product.get(idProduct)

        //Let's initialize the values of the cart_item
        BigDecimal totalPrice = product.price*quantity
        def now = new Date()
        CartItem cartItem = new CartItem(product: product, quantity: quantity, totalPrice: totalPrice)

        if(cart != null){
            def command = Cart.executeQuery("select item from Cart cart " +
                    "inner join cart.items item " +
                    "where cart.id = :idCart and item.product.id = :idProduct",
                    [idCart:cart.id, idProduct:idProduct])
            command.each { item ->
                println("ITEM : {\n QTY : "+item.getAt("quantity")+"\n TP : "+item.getAt("totalPrice")+"\n}")
            }

            if(command){
                Integer qty = quantity
                BigDecimal cartItemTotalPrice = totalPrice
                command.each { item ->
                    qty = qty+item.getAt("quantity")
                    cartItemTotalPrice = cartItemTotalPrice+item.getAt("totalPrice")
                }
                if(qty>0) {
                    CartItem.executeUpdate("UPDATE CartItem command SET command.quantity = :qty, command.totalPrice = :totalPrice, command.lastUpdatedDate = :lastUpdatedDate " +
                            "WHERE command.product.id = :idProduct",
                            [qty: qty, totalPrice: cartItemTotalPrice, lastUpdatedDate: new Date(), idProduct: product.id])
                    user.cart.totalPrice = user.cart.totalPrice + totalPrice
                    user.cart.save(flush:true, failOnError: true)
                } else {
                    println("Quantity must be larger than the one in the cart !!!\nOr just delete the item to remove it from the cart")
                }
            } else {
                if(quantity>0) {
                    cartItem.creationDate = now
                    cartItem.lastUpdatedDate = now
                    cartItem.save(failOnError: true)

                    user.cart.addToItems(cartItem)
                    user.cart.totalPrice = user.cart.totalPrice + totalPrice
                    user.cart.save(flush:true, failOnError: true)
                } else {
                    println("Quantity must be positive !!!")
                }
            }
            //user.cart.save(flush:true)
        } else {
            if(quantity>0){
                cartItem.creationDate = now
                cartItem.lastUpdatedDate = now
                cartItem.save(failOnError: true)


                def myNewCart = new Cart(totalPrice: totalPrice, user:user)
                myNewCart.addToItems(cartItem)
                myNewCart.save(flush:true, failOnError: true)

                user.cart = myNewCart
                user.save(flush:true, failOnError: true)


                println("USER CART : { \n   ITEMS : "+user.cart.items[0].product.name+"\n   TOTAL PRICE : "+user.cart.totalPrice+"\n}")
            } else {
                println("Quantity must be positive !!!")
            }
        }

    }

    def removeItemFromCart(Long idUser, Long idProduct){
        User user = User.get(idUser)
        Cart cart = user.cart
        def command = Cart.executeQuery("select item from Cart cart " +
                "inner join cart.items item " +
                "where cart.id = :idCart and item.product.id = :idProduct",
                [idCart: cart.id, idProduct: idProduct])
        if(command!=null){
            Long id = 0
            BigDecimal totalPrice = 0
            command.each { item ->
                id = item.getAt("id")
                totalPrice = item.getAt("totalPrice")
            }
            def commandToRemove = cart.items.find { it.id == id }
            cart.removeFromItems(commandToRemove)
            user.cart.totalPrice = user.cart.totalPrice - totalPrice
            user.cart.save(flush:true, failOnError: true)
            CartItem.executeUpdate("DELETE FROM CartItem command WHERE command.id = :id", [id: id])
        }
    }
}