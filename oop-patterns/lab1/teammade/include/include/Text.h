#ifndef TEXT_H
#define TEXT_H

#include <string>
#include <vector>
#include <sstream>
#include <algorithm>

class Text {
public:
    virtual ~Text() = default;
    virtual std::string getContent() const = 0;
};

class NameText : public Text {
private:
    std::string formattedName;
    std::string initials;
    size_t rawLength;
    bool valid; // fix: private 블록과 멤버변수가 2번 선언되어 있었고 parse()도 2번 정의 + 두번째는 잘려있었음 → 하나로 통합

public:
    NameText(const std::string& rawInput) : rawLength(rawInput.length()), valid(false) {
        parse(rawInput);
    }

    void parse(const std::string& input) {
        rawLength = input.length();
        if (rawLength > 28) {
            valid = false;
            return;
        }

        std::vector<std::string> tokens;
        std::stringstream ss(input);
        std::string token;
        while (ss >> token) {
            tokens.push_back(token);
        }

        if (tokens.size() == 2) { // fix: 원래 tokens.size()!=3이면 무조건 invalid 처리 → 2단어("Barbara Thomson") 지원 추가
            std::string first = tokens[0];
            std::string last = tokens[1];

            formattedName = first + " " + last;

            std::string fullInitials = "";
            fullInitials += (char)toupper(first[0]);
            fullInitials += (char)toupper(last[0]);
            initials = fullInitials;
            valid = true;
        } else if (tokens.size() == 3) {
            std::string first = tokens[0];
            std::string middle = tokens[1];
            std::string last = tokens[2];

            formattedName = first + " " + (char)toupper(middle[0]) + " " + last;

            std::string fullInitials = "";
            fullInitials += (char)toupper(first[0]);
            fullInitials += (char)toupper(middle[0]);
            fullInitials += (char)toupper(last[0]);
            initials = fullInitials;
            valid = true;
        } else {
            valid = false;
        }
    }

    bool checkValid() const { return valid; }
    std::string getContent() const override { return formattedName; }
    std::string getInitials() const { return initials; }
    size_t getRawInputLength() const { return rawLength; }
};

#endif // TEXT_H
