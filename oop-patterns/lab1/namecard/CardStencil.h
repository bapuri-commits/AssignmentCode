#ifndef CARDSTENCIL_H
#define CARDSTENCIL_H

#include <string>
using namespace std;

// 카드 형태에 대한 추상 클래스
class CardStencil {
protected:
    char borderChar;
    int width;

public:
    CardStencil() : borderChar('*'), width(32) {}
    virtual ~CardStencil() {}

    char getBorderChar() { return borderChar; }
    void setBorderChar(char c) { borderChar = c; }
    int getWidth() { return width; }

    virtual string buildTopLine(string initials) = 0;
    virtual string buildNameLine(string displayName) = 0;
    virtual string buildEmptyLine() = 0;
};

// 32자 폭 카드 양식
class CardStencil1 : public CardStencil {
public:
    CardStencil1() : CardStencil() {}

    string buildTopLine(string initials) override {
        int fillCount = width - 2 * (int)initials.length() + 2;
        string fill(fillCount, borderChar);
        return initials + fill + initials;
    }

    string buildNameLine(string displayName) override {
        int totalSpaces = width - 2 - displayName.length();
        int leftSpaces = (totalSpaces + 1) / 2;
        int rightSpaces = totalSpaces / 2;

        string left(leftSpaces, ' ');
        string right(rightSpaces, ' ');
        return borderChar + left + displayName + right + borderChar;
    }

    string buildEmptyLine() override {
        string spaces(width - 2, ' ');
        return borderChar + spaces + borderChar;
    }
};

#endif
