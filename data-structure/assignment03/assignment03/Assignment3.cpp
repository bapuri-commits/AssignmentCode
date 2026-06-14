#include <stdio.h>
#include <stdlib.h>

// ===== 원형큐 코드 시작 ======
#define MAX_QUEUE_SIZE 5000
typedef struct {
    int index;
    int level;
    int zigzagsum;
}instance;
typedef instance element;
typedef struct {       // 큐 타입
    element data[MAX_QUEUE_SIZE];
    int front, rear;
} QueueType;

// 오류 함수
void error(const char* message)
{
    fprintf(stderr, "%s\n", message);
    exit(1);
}

// 초기화 함수
void init_queue(QueueType* q)
{
    q->front = q->rear = 0;
}

// 공백 상태 검출 함수
int is_empty(QueueType* q)
{
    return (q->front == q->rear);
}

// 포화 상태 검출 함수
int is_full(QueueType* q)
{
    return ((q->rear + 1) % MAX_QUEUE_SIZE == q->front);
}

// 원형큐 출력 함수
void queue_print(QueueType* q)
{
    printf("QUEUE(front=%d rear=%d) = ", q->front, q->rear);
    if (!is_empty(q)) {
        int i = q->front;
        do {
            i = (i + 1) % (MAX_QUEUE_SIZE);
            printf("%d | ", q->data[i]);
            if (i == q->rear)
                break;
        } while (i != q->front);
    }
    printf("\n");
}

// 삽입 함수
void enqueue(QueueType* q, element item)
{
    if (is_full(q))
        error("큐가 포화상태입니다");
    q->rear = (q->rear + 1) % MAX_QUEUE_SIZE;
    q->data[q->rear] = item;
}

// 삭제 함수
element dequeue(QueueType* q)
{
    if (is_empty(q))
        error("큐가 공백상태입니다");
    q->front = (q->front + 1) % MAX_QUEUE_SIZE;
    return q->data[q->front];
}

// peek 함수
element peek(QueueType* q)
{
    if (is_empty(q))
        error("큐가 공백상태입니다");
    return q->data[(q->front + 1) % MAX_QUEUE_SIZE];
}

int Step = 0;

int main(void) {
    QueueType tree;
    init_queue(&tree);
    QueueType subtree;
    init_queue(&subtree);
   
    int treearr[5000]; 

    int num,target;
    scanf("%d",&num);
    for (int i = 0; i < num; i++)
    {
        scanf("%d",&treearr[i]);
    }
    scanf("%d", &target);
    if (num == 0 || treearr[0] == -1) {
        printf("NO tree given\n");
        return 0;
    }
    instance root = { .index = 0, .level = 0, .zigzagsum = treearr[0] };
    enqueue(&tree, root);
    while (!is_empty(&tree)) {
        instance cur = dequeue(&tree);
        printf("[STEP %d] L=%d, node=%d, zigzag_sum=%d\n",Step,cur.level,treearr[cur.index],cur.zigzagsum);
        Step++;
        int l, r;
        l = cur.index * 2 + 1;
        r = l + 1;
        if (l<num && treearr[l] != -1) {
            instance left;
            left = { .index = l,.level = cur.level + 1, .zigzagsum = cur.zigzagsum + ((cur.level + 1) % 2 == 1 ? treearr[l] : 0 )};
            enqueue(&tree, left);
        }
        if (r<num && treearr[r] != -1) {
            instance right;
            right = { .index = r,.level = cur.level + 1, .zigzagsum = cur.zigzagsum + ((cur.level + 1) % 2 == 0 ? treearr[r] : 0) };
            enqueue(&tree, right);
        }
        
        
    }

}
