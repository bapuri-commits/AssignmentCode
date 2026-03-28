#ifndef ORDER_H
#define ORDER_H

#include "Product.h"
#include <memory>

class Order {
public:
    virtual ~Order() = default;
    virtual std::shared_ptr<Product> getProduct() const = 0;
    virtual int getQuantity() const = 0;
    virtual int getTotalPrice() const = 0;
};

#endif // ORDER_H
