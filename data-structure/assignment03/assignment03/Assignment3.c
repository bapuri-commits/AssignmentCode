
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
void error(char* message)
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
/*void queue_print(QueueType* q)
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
}*/
// 삽입 함수
void enqueue(QueueType* q, element item)
{
    if (is_full(q))error("Queue is full");
    q->rear = (q->rear + 1) % MAX_QUEUE_SIZE;
    q->data[q->rear] = item;
}
// 삭제 함수
element dequeue(QueueType* q)
{
    if (is_empty(q))error("Queue is empty");
    q->front = (q->front + 1) % MAX_QUEUE_SIZE;
    return q->data[q->front];
}
// peek 함수
element peek(QueueType* q)
{
    if (is_empty(q))error("Queue is empty");
    return q->data[(q->front + 1) % MAX_QUEUE_SIZE];
}
int Step = 1;
int main(void) {
    QueueType tree;
    init_queue(&tree);
    QueueType subtree;
    int treearr[5000];
    int levelsum[20];
    for (int i = 0; i < 20; i++)
    {
        levelsum[i] = 0;
    }
    int num, target;
    int targetparent, targetlevel;
    int targetlsib = -1, targetrsib = -1;
    int inbalancenodes[20]; int inbalanceidx = 0; int inbalancecount = 0;
    int found = 0;
    scanf("%d", &num);
    for (int i = 0; i < num; i++)
    {
        scanf("%d", &treearr[i]);
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
        levelsum[cur.level] += treearr[cur.index];
        if (!found && treearr[cur.index] == target) {
            found = 1;
            if(cur.index > 0){ targetparent = treearr[(cur.index - 1) / 2]; }
            else { targetparent = -1; }
            targetlevel = cur.level; 
            if (cur.index % 2 == 1 && cur.index + 1 < num) targetrsib = treearr[cur.index + 1];
            else if(cur.index % 2 == 0 && cur.index > 0) targetlsib = treearr[cur.index - 1];
            else { targetlsib = -1; targetrsib = -1; }
        }
        printf("[STEP %d] L=%d, node=%d, zigzag_sum=%d\n", Step, cur.level, treearr[cur.index], cur.zigzagsum);
        Step++;
        int l, r;
        l = cur.index * 2 + 1;
        r = l + 1;
        int lnodenum = 0, rnodenum = 0;
        if (l < num && treearr[l] != -1) {
            instance left = { .index = l,.level = cur.level + 1, .zigzagsum = cur.zigzagsum + ((cur.level + 1) % 2 == 1 ? treearr[l] : 0) };
            init_queue(&subtree);
            enqueue(&subtree,left);
            
            while (!is_empty(&subtree)) {
                instance lcur = dequeue(&subtree);
                lnodenum++;
                int subl = (lcur.index) * 2 + 1, subr = subl + 1;
                if(subl < num && treearr[subl] != -1){
                    instance subleft = { .index = subl , .level = lcur.level + 1, .zigzagsum = 0 };//지그재그는 중요하지 않으니 0으로 취급
                    enqueue(&subtree,subleft);
                    
                }
                if (subr < num && treearr[subr] != -1) {
                    instance subright = { .index = subr , .level = lcur.level + 1, .zigzagsum = 0 };//지그재그는 중요하지 않으니 0으로 취급
                    enqueue(&subtree, subright);
                    
                }
            }
            enqueue(&tree, left);
        }
        if (r < num && treearr[r] != -1) {
            instance right = { .index = r,.level = cur.level + 1, .zigzagsum = cur.zigzagsum + ((cur.level + 1) % 2 == 0 ? treearr[r] : 0) };
            init_queue(&subtree);
            enqueue(&subtree, right);
            
            while (!is_empty(&subtree)) {
                instance rcur = dequeue(&subtree);
                rnodenum++;
                int subl = (rcur.index) * 2 + 1, subr = subl + 1;
                if (subl < num && treearr[subl] != -1) {
                    instance subleft = { .index = subl , .level = rcur.level + 1, .zigzagsum = 0 };//지그재그는 중요하지 않으니 0으로 취급
                    enqueue(&subtree, subleft);
                    
                }
                if (subr < num && treearr[subr] != -1) {
                    instance subright = { .index = subr , .level = rcur.level + 1, .zigzagsum = 0 };//지그재그는 중요하지 않으니 0으로 취급
                    enqueue(&subtree, subright);
                    
                }
            }
            enqueue(&tree, right);
        }
        if (lnodenum - rnodenum >= 2 || lnodenum - rnodenum <= -2) {
            inbalancenodes[inbalanceidx++] = treearr[cur.index]; inbalancecount++;
        }
    }
    printf("=== Level Sums ===\n");
    int j = 0;
    while (levelsum[j] != 0) {
        printf("L%d: %d\n", j, levelsum[j]);
        j++;
    }
    printf("=== Target ===\n");
    if (!found) {
        printf("NO target found\n");
    }
    else {
        if (targetparent != -1)printf("Found target %d at L=%d, parent=%d, ", target, targetlevel, targetparent);
        else printf("Found target %d at L=%d, parent=NIL, ", target, targetlevel);
        if (targetlsib == -1)printf("lsib=NIL, ");
        else printf("lsib=%d, ", targetlsib);
        if (targetrsib == -1)printf("rsib=NIL\n");
        else printf("rsib=%d\n", targetrsib);
    }
    printf("=== Imbalanced Nodes ===\n");
    printf("count: %d\n",inbalancecount);
    printf("nodes:");
    if (inbalancecount) { for (int i = 0; i < inbalancecount; i++) { printf(" %d", inbalancenodes[i]); }; }
    else { printf(" (none)"); }
    printf("\n");
}