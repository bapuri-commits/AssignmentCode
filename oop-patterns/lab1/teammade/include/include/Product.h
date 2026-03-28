#ifndef PRODUCT_H
#define PRODUCT_H

#include <string>

class Product {
public:
    virtual ~Product() = default;
    virtual std::string getName() const = 0;
    virtual int getPrice() const = 0;
    virtual std::string toString() const = 0;
};

#endif // PRODUCT_H
