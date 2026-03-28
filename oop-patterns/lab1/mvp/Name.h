#ifndef NAME_H
#define NAME_H

#include <string>
using namespace std;

class Name {
private:
    string firstName;
    string middleName;
    string lastName;
    string fullInput;

public:
    Name(string fullName) {
        fullInput = fullName;

        int space1 = fullName.find(' ');
        if (space1 == string::npos) {
            firstName = fullName;
            middleName = "";
            lastName = "";
            return;
        }

        firstName = fullName.substr(0, space1);
        string rest = fullName.substr(space1 + 1);

        int space2 = rest.find(' ');
        if (space2 == string::npos) {
            middleName = "";
            lastName = rest;
        } else {
            middleName = rest.substr(0, space2);
            lastName = rest.substr(space2 + 1);
        }
    }

    string getInitials() {
        string initials = "";
        initials += toupper(firstName[0]);
        if (!middleName.empty()) {
            initials += toupper(middleName[0]);
        }
        initials += toupper(lastName[0]);
        return initials;
    }

    string getDisplayName() {
        if (!middleName.empty()) {
            return firstName + " " + (char)toupper(middleName[0]) + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    int getDisplayLength() {
        return getDisplayName().length();
    }

    bool isValid() {
        return fullInput.find(' ') != string::npos && fullInput.length() <= 28;
    }
};

#endif
