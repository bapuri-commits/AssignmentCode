#ifndef CARD_STENCIL_H
#define CARD_STENCIL_H

#include <string>
#include <vector>

class CardStencil {
protected:
    char borderChar = '*';

public:
    virtual ~CardStencil() = default;
    virtual std::string getPattern() const = 0;
    virtual std::string render(const std::string& name, const std::string& initials) const = 0;
    void setBorderChar(char c) { borderChar = c; }
};

class CardStencil1 : public CardStencil {
public:
    std::string getPattern() const override {
        return "Standard-Stencil-Pattern-V1";
    }

    std::string render(const std::string& name, const std::string& initials) const override {
        const int width = 32;
        std::string result = "";
        
        // Header (JPM**************************JPM)
        // initials는 각 단어의 첫 글자를 딴 이니셜 (예: Jonathan P Macdonald -> JPM)
        int middlePatternLen = width - (static_cast<int>(initials.length()) * 2);
        std::string line1 = initials + std::string(middlePatternLen, borderChar) + initials + "\n";
        result += line1;
        
        // Empty lines (before name)
        result += borderChar + std::string(width - 2, ' ') + borderChar + "\n";
        
        // Name line (centered)
        int paddingLen = width - 2 - static_cast<int>(name.length());
        int leftPad = paddingLen / 2;
        if (paddingLen % 2 != 0) {
            // 공백이 홀수 개라면 여분의 공백은 이름 앞에 넣는다.
            leftPad++; 
        }
        int rightPad = width - 2 - static_cast<int>(name.length()) - leftPad;
        
        result += borderChar + std::string(leftPad, ' ') + name + std::string(rightPad, ' ') + borderChar + "\n";
        
        // Empty lines (after name)
        result += borderChar + std::string(width - 2, ' ') + borderChar + "\n";
        
        // Footer (same as header)
        result += line1;

        return result;
    }
};

#endif // CARD_STENCIL_H
