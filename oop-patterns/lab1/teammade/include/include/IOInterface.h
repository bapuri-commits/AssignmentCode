#ifndef IO_INTERFACE_H
#define IO_INTERFACE_H

#include <string>
#include <vector>
#include <queue>

class Input {
public:
    virtual ~Input() = default;
    virtual std::string readLine() = 0;
};

class Output {
public:
    virtual ~Output() = default;
    virtual void writeLine(const std::string& msg) = 0;
};

// 실제 사용자를 위한 구현체
class UserInput : public Input {
public:
    std::string readLine() override {
        std::string line;
        std::getline(std::cin, line);
        return line;
    }
};

class UserOutput : public Output {
public:
    void writeLine(const std::string& msg) override {
        std::cout << msg << std::endl;
    }
};

// 테스트를 위한 Mock 구현체
class TestInput : public Input {
private:
    std::queue<std::string> inputs;
public:
    void pushInput(const std::string& input) {
        inputs.push(input);
    }
    std::string readLine() override {
        if (inputs.empty()) return "";
        std::string val = inputs.front();
        inputs.pop();
        return val;
    }
};

class TestOutput : public Output {
private:
    std::vector<std::string> outputs;
public:
    void writeLine(const std::string& msg) override {
        outputs.push_back(msg);
    }
    const std::vector<std::string>& getOutputs() const {
        return outputs;
    }
};

#endif // IO_INTERFACE_H
