package test

import grails.rest.Resource

class Product {
    String name
    Float price
    int inStock
    String description
    String photo

    static belongsTo = [category: Category]

    static constraints = {
        name nullable: false, blank: false
        price min: 0F, scale: 2
        inStock nullable: false, blank: false
    }
}
