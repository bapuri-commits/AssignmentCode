#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#define MAX_STACK_SIZE 100

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

void push_int(IntStackType *s, element item) {
    s->data[++(s->top)] = item;
}

element pop_int(IntStackType *s) {
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

void push_char(CharStackType *s, op_element item) {
    s->data[++(s->top)] = item;
}

op_element pop_char(CharStackType *s) {
    return s->data[(s->top)--];
}

int main(void){

}