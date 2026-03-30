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
    bool isValid;

public:
    NameText(const std::string& rawInput) : isValid(false) {
        parse(rawInput);
    }

    void parse(const std::string& input) {
        if (input.length() > 28) {
            isValid = false;
            return;
        }

        std::vector<std::string> tokens;
        std::stringstream ss(input);
        std::string token;
        while (ss >> token) {
            tokens.push_back(token);
        }

        if (tokens.size() != 3) {
            isValid = false;
            return;
        }

        // Format: First Name, Middle Name, Last Name
        // Input: "Eun Man Choi" -> "Eun M Choi"
        std::string first = tokens[0];
        std::string middle = tokens[1];
        std::string last = tokens[2];
        
        formattedName = first + " " + middle[0] + " " + last;
        
        std::string fullInitials = "";
        fullInitials += (char)toupper(first[0]);
        fullInitials += (char)toupper(middle[0]);
        fullInitials += (char)toupper(last[0]);

        initials = fullInitials;
        isValid = true;
    }

    bool checkValid() const { return isValid; }
    std::string getContent() const override { return formattedName; }
    std::string getInitials() const { return initials; }
    size_t getRawInputLength() const { return rawLength; }

private:
    std::string formattedName;
    std::string initials;
    size_t rawLength;
    bool isValid;

public:
    void parse(const std::string& input) {
        rawLength = input.length();
        if (rawLength > 28) {
            isValid = false;
            return;
        }

#endif // TEXT_H
