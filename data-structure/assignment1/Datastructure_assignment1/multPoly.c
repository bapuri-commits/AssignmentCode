#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#define MAX_TERMS 101

typedef struct {
	float coef;
	int xexp, yexp, zexp;
} polynomial;

typedef struct {
	polynomial factors[10];
	int factorsnum;
} Teamgroup;

polynomial terms[MAX_TERMS];
int avail = 0;

/* ===== test case 1 ===== */
/* A: 3y^7 + 4x^4 - 1 */
Teamgroup A1[] = {
	{ {{3, 0, 7, 0}}, 1 },
	{ {{4, 4, 0, 0}}, 1 },
	{ {{-1, 0, 0, 0}}, 1 }
};
/* B: 5x^4 - 3y^2 + 7 */
Teamgroup B1[] = {
	{ {{5, 4, 0, 0}}, 1 },
	{ {{-3, 0, 2, 0}}, 1 },
	{ {{7, 0, 0, 0}}, 1 }
};

/* ===== test case 2 ===== */
/* A: 4y^7 - 4x^4 - 1 */
Teamgroup A2[] = {
	{ {{4, 0, 7, 0}}, 1 },
	{ {{-4, 4, 0, 0}}, 1 },
	{ {{-1, 0, 0, 0}}, 1 }
};
/* B: 2y^7 - 3x + 1 */
Teamgroup B2[] = {
	{ {{2, 0, 7, 0}}, 1 },
	{ {{-3, 1, 0, 0}}, 1 },
	{ {{1, 0, 0, 0}}, 1 }
};

/* ===== test case 3 (extreme) ===== */
/* A: 15x^4 * y^7 - 9y^9 + 21y^7 + 20x^8 */
Teamgroup A3[] = {
	{ {{15, 4, 0, 0}, {1, 0, 7, 0}}, 2 },
	{ {{-9, 0, 9, 0}}, 1 },
	{ {{21, 0, 7, 0}}, 1 },
	{ {{20, 8, 0, 0}}, 1 }
};
/* B: -12x^4 * y^2 + 23x^4 + 3y^2 - 7 */
Teamgroup B3[] = {
	{ {{-12, 4, 0, 0}, {1, 0, 2, 0}}, 2 },
	{ {{23, 4, 0, 0}}, 1 },
	{ {{3, 0, 2, 0}}, 1 },
	{ {{-7, 0, 0, 0}}, 1 }
};

/* ===== test case 4 (extreme2) ===== */
/* A: 15x^4 * y^7 * 6z^3 - 9y^9 * z^2 + 21y^7 + 20x^8 */
Teamgroup A4[] = {
	{ {{15, 4, 0, 0}, {1, 0, 7, 0}, {6, 0, 0, 3}}, 3 },
	{ {{-9, 0, 9, 0}, {1, 0, 0, 2}}, 2 },
	{ {{21, 0, 7, 0}}, 1 },
	{ {{20, 8, 0, 0}}, 1 }
};
/* B: -12x^4 * y^2 * z^2 + 23x^4 + 3y^2 - 7 */
Teamgroup B4[] = {
	{ {{-12, 4, 0, 0}, {1, 0, 2, 0}, {1, 0, 0, 2}}, 3 },
	{ {{23, 4, 0, 0}}, 1 },
	{ {{3, 0, 2, 0}}, 1 },
	{ {{-7, 0, 0, 0}}, 1 }
};

int compare_y(const void *a, const void *b)
{
	polynomial *ta = (polynomial *)a;
	polynomial *tb = (polynomial *)b;
	return tb->yexp - ta->yexp;
}

void attach(float coef, int xexp, int yexp, int zexp)
{
	if (avail >= MAX_TERMS) {
		fprintf(stderr, "Terms are so many\n");
		exit(1);
	}
	terms[avail].coef = coef;
	terms[avail].xexp = xexp;
	terms[avail].yexp = yexp;
	terms[avail].zexp = zexp;
	avail++;
}

void multPoly(Teamgroup A[], int sizeA, Teamgroup B[], int sizeB, int *cs, int *ce)
{
	polynomial temp[MAX_TERMS];
	int cnt = 0;

	for (int i = 0; i < sizeA; i++) {
		for (int j = 0; j < sizeB; j++) {
			float c = 1.0f;
			int xe = 0, ye = 0, ze = 0;

			for (int k = 0; k < A[i].factorsnum; k++) {
				c  *= A[i].factors[k].coef;
				xe += A[i].factors[k].xexp;
				ye += A[i].factors[k].yexp;
				ze += A[i].factors[k].zexp;
			}
			for (int k = 0; k < B[j].factorsnum; k++) {
				c  *= B[j].factors[k].coef;
				xe += B[j].factors[k].xexp;
				ye += B[j].factors[k].yexp;
				ze += B[j].factors[k].zexp;
			}

			temp[cnt].coef = c;
			temp[cnt].xexp = xe;
			temp[cnt].yexp = ye;
			temp[cnt].zexp = ze;
			cnt++;
		}
	}

	*cs = avail;
	for (int i = 0; i < cnt; i++) {
		if (temp[i].coef == 0) continue;
		float c = temp[i].coef;
		for (int j = i + 1; j < cnt; j++) {
			if (temp[j].xexp == temp[i].xexp &&
			    temp[j].yexp == temp[i].yexp &&
			    temp[j].zexp == temp[i].zexp) {
				c += temp[j].coef;
				temp[j].coef = 0;
			}
		}
		if (c != 0)
			attach(c, temp[i].xexp, temp[i].yexp, temp[i].zexp);
	}
	*ce = avail - 1;

	if (*ce >= *cs)
		qsort(&terms[*cs], *ce - *cs + 1, sizeof(polynomial), compare_y);
}

void print_poly(int s, int e)
{
	if (s > e) {
		printf("0\n");
		return;
	}
	for (int i = s; i <= e; i++) {
		if (i == s)
			printf("%.1f", terms[i].coef);
		else if (terms[i].coef >= 0)
			printf(" + %.1f", terms[i].coef);
		else
			printf(" - %.1f", -terms[i].coef);

		if (terms[i].xexp == 1) printf("x");
		else if (terms[i].xexp > 1) printf("x^%d", terms[i].xexp);

		if (terms[i].yexp == 1) printf("y");
		else if (terms[i].yexp > 1) printf("y^%d", terms[i].yexp);

		if (terms[i].zexp == 1) printf("z");
		else if (terms[i].zexp > 1) printf("z^%d", terms[i].zexp);
	}
	printf("\n");
}

int main(void)
{
	int Cs, Ce;

	printf("1) test case\n");
	printf("A: 3y^7 + 4x^4 - 1\n");
	printf("B: 5x^4 - 3y^2 + 7\n");
	avail = 0;
	multPoly(A1, sizeof(A1)/sizeof(A1[0]), B1, sizeof(B1)/sizeof(B1[0]), &Cs, &Ce);
	printf("A * B = ");
	print_poly(Cs, Ce);
	printf("\n");

	printf("2) general case\n");
	printf("A: 4y^7 - 4x^4 - 1\n");
	printf("B: 2y^7 - 3x + 1\n");
	avail = 0;
	multPoly(A2, sizeof(A2)/sizeof(A2[0]), B2, sizeof(B2)/sizeof(B2[0]), &Cs, &Ce);
	printf("A * B = ");
	print_poly(Cs, Ce);
	printf("\n");

	printf("3) extreme case\n");
	printf("A: 15x^4 * y^7 - 9y^9 + 21y^7 + 20x^8\n");
	printf("B: -12x^4 * y^2 + 23x^4 + 3y^2 - 7\n");
	avail = 0;
	multPoly(A3, sizeof(A3)/sizeof(A3[0]), B3, sizeof(B3)/sizeof(B3[0]), &Cs, &Ce);
	printf("A * B = ");
	print_poly(Cs, Ce);
	printf("\n");

	printf("4) extreme case2\n");
	printf("A: 15x^4 * y^7 * 6z^3 - 9y^9 * z^2 + 21y^7 + 20x^8\n");
	printf("B: -12x^4 * y^2 * z^2 + 23x^4 + 3y^2 - 7\n");
	avail = 0;
	multPoly(A4, sizeof(A4)/sizeof(A4[0]), B4, sizeof(B4)/sizeof(B4[0]), &Cs, &Ce);
	printf("A * B = ");
	print_poly(Cs, Ce);

	return 0;
}
