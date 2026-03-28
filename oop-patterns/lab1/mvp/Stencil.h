#ifndef STENCIL_H
#define STENCIL_H

#include <string>
using namespace std;

class Stencil {
private:
    char borderChar;
    int width;

public:
    Stencil() {
        borderChar = '*';
        width = 32;
    }

    char getBorderChar() { return borderChar; }
    void setBorderChar(char c) { borderChar = c; }

    string buildTopLine(string initials) {
        int fillCount = width - 2 * (int)initials.length() + 2;
        string fill(fillCount, borderChar);
        return initials + fill + initials;
    }

    string buildEmptyLine() {
        string spaces(width - 2, ' ');
        return borderChar + spaces + borderChar;
    }

    string buildNameLine(string displayName) {
        int totalSpaces = width - 2 - displayName.length();
        int leftSpaces = (totalSpaces + 1) / 2;
        int rightSpaces = totalSpaces / 2;

        string left(leftSpaces, ' ');
        string right(rightSpaces, ' ');
        return borderChar + left + displayName + right + borderChar;
    }
};

#endif
