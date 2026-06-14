#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#define MAX_EXPRESSION_SIZE 100
#define MAX_STACK_SIZE 100

char expression[MAX_EXPRESSION_SIZE];
char finalexpression[MAX_EXPRESSION_SIZE][MAX_EXPRESSION_SIZE];

int tokennumber;

//피연산자 스택
typedef int element;
typedef struct {
    element data[MAX_STACK_SIZE];
    int top;
} IntStackType;

void init_int_stack(IntStackType *s) {
    s->top = -1;
}

int is_int_empty(IntStackType *s) {
    return (s->top == -1);
}

int is_int_full(IntStackType *s) {
    return (s->top == (MAX_STACK_SIZE - 1));
}

void push_int(IntStackType *s, element item) {
    if (is_int_full(s)) {
        fprintf(stderr, "스택 포화 에러\n");
        return;
    }
    s->data[++(s->top)] = item;
}

element pop_int(IntStackType *s) {
    if (is_int_empty(s)) {
        fprintf(stderr, "스택 공백 에러\n");
        exit(1);
    }
    return s->data[(s->top)--];
}

//연산자 스택
typedef char op_element;
typedef struct {
    op_element data[MAX_STACK_SIZE];
    int top;
} CharStackType;

void init_char_stack(CharStackType *s) {
    s->top = -1;
}

int is_char_empty(CharStackType *s) {
    return (s->top == -1);
}

int is_char_full(CharStackType *s) {
    return (s->top == (MAX_STACK_SIZE - 1));
}

void push_char(CharStackType *s, op_element item) {
    if (is_char_full(s)) {
        fprintf(stderr, "스택 포화 에러\n");
        return;
    }
    s->data[++(s->top)] = item;
}

op_element pop_char(CharStackType *s) {
    if (is_char_empty(s)) {
        fprintf(stderr, "스택 공백 에러\n");
        exit(1);
    }
    return s->data[(s->top)--];
}

int isnum(char c) {
    int st = c - '0';
    return (st <= 9 && st >= 0);
}

int prefix2StackEval() {
    IntStackType operand;
    init_int_stack(&operand);
    CharStackType opr;
    init_char_stack(&opr);

    int operand_snapshot[MAX_STACK_SIZE][MAX_STACK_SIZE] = {0};
    char operator_snapshot[MAX_STACK_SIZE][MAX_STACK_SIZE] = {0};
    int operand_top[MAX_STACK_SIZE];
    int operator_top[MAX_STACK_SIZE];
    int operand_top_max = -1;
    int operator_top_max = -1;
    int step = 0;
    for (int k = 0; k < MAX_STACK_SIZE; k++) {
        operand_top[k] = -1;
        operator_top[k] = -1;
    }

    for (int i = tokennumber; i >= 0; i--)
    {
        char* cur = finalexpression[i];
        int buffer = 0;
        if (isnum(cur[0])) {
            int j = 0;
            while(cur[j]) {
                int digit = cur[j++]-'0';
                buffer = buffer * 10 + digit;
            }
            push_int(&operand, buffer);
        }
        else {
            push_char(&opr, cur[0]);
        }

        for (int k = 0; k <= operand.top; k++)
            operand_snapshot[step][k] = operand.data[k];
        operand_top[step] = operand.top;
        operand_top_max = (operand.top > operand_top_max) ? operand.top : operand_top_max;

        for (int k = 0; k <= opr.top; k++)
            operator_snapshot[step][k] = opr.data[k];
        operator_top[step] = opr.top;
        operator_top_max = (opr.top > operator_top_max) ? opr.top : operator_top_max;

        step++;

        while (operand.top >= 1 && !is_char_empty(&opr)) {
            int a = pop_int(&operand);
            int b = pop_int(&operand);
            int ans;
            char op = pop_char(&opr);
            switch (op)
            {
            case '+':
                ans = a + b;
                break;
            case '-':
                ans = a - b;
                break;
            case '*':
                ans = a * b;
                break;
            case '/':
                ans = a / b;
                break;
            default:
                break;
            }
            push_int(&operand, ans);

            for (int k = 0; k <= operand.top; k++)
                operand_snapshot[step][k] = operand.data[k];
            operand_top[step] = operand.top;
            operand_top_max = (operand.top > operand_top_max) ? operand.top : operand_top_max;
            for (int k = 0; k <= opr.top; k++)
                operator_snapshot[step][k] = opr.data[k];
            operator_top[step] = opr.top;
            operator_top_max = (opr.top > operator_top_max) ? opr.top : operator_top_max;
            step++;
        }
    }
    printf("피연산자 스택:\n");
    for (int i = operand_top_max; i >= -1; i--) {
        for (int j = 0; j < step; j++) {
            if (i == -1) { printf("=== "); continue; }
            if (i <= operand_top[j]) printf("%3d ", operand_snapshot[j][i]);
            else printf("    ");
        }
        printf("\n");
    }
    printf("연산자 스택:\n");
    for (int i = operator_top_max; i >= -1; i--) {
        for (int j = 0; j < step; j++) {
            if (i == -1) { printf("=== "); continue; }
            if (i <= operator_top[j]) printf("%3c ", operator_snapshot[j][i]);
            else printf("    ");
        }
        printf("\n");
    }
    return pop_int(&operand);
}

int strLen(char* e) {
    int i = 0;
    while (e[i]) { i++; }
    return i;
}

int parsingToken(char* full, char (*final)[MAX_EXPRESSION_SIZE]) {
    int fullsize = strLen(full);
    int token = 0, idx = 0;
    for (int i = 0; i < fullsize; i++)
    {
        char cur = full[i];
        if (isnum(cur)) { final[token][idx++] = cur; }
        else {
            switch (cur) {
            case ' ':
                if (idx > 0) { final[token][idx] = '\0'; token++; idx = 0; }
                break;
            default:
                final[token][idx++] = cur;
                break;
            }
        }
    }
    if (idx > 0) { final[token][idx] = '\0'; token++; }
    return token;
}

int main(void){
    printf("전위표현식 (최대 길이 = 100) : ");
    scanf("%[^\n]", expression);
    tokennumber = parsingToken(expression, finalexpression) - 1;
    int result = prefix2StackEval();
    printf("최종결과 : %d\n", result);
    return 0;
}
