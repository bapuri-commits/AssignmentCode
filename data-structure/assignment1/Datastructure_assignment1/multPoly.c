#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#define MAX_TERMS 101

typedef struct
{
	float coef;
	int xexp,yexp,zexp;
}polynomial;

typedef struct {
	polynomial factors[10];
	int factorsnum;
}Teamgroup;

int avail = 0;

// test case 1
// A: 3y^7 + 4x^4 - 1
Teamgroup A1[] = {
	{ {{3, 0, 7, 0}}, 1 },
	{ {{4, 4, 0, 0}}, 1 },
	{ {{-1, 0, 0, 0}}, 1 }
};
// B: 5x^4 - 3y^2 + 7
Teamgroup B1[] = {
	{ {{5, 4, 0, 0}}, 1 },
	{ {{-3, 0, 2, 0}}, 1 },
	{ {{7, 0, 0, 0}}, 1 }
};

// test case 2
// A: 4y^7 - 4x^4 - 1
Teamgroup A2[] = {
	{ {{4, 0, 7, 0}}, 1 },
	{ {{-4, 4, 0, 0}}, 1 },
	{ {{-1, 0, 0, 0}}, 1 }
};
// B: 2y^7 - 3x + 1
Teamgroup B2[] = {
	{ {{2, 0, 7, 0}}, 1 },
	{ {{-3, 1, 0, 0}}, 1 },
	{ {{1, 0, 0, 0}}, 1 }
};

// test case 3 (extreme)
// A: 15x^4 * y^7 - 9y^9 + 21y^7 + 20x^8
Teamgroup A3[] = {
	{ {{15, 4, 7, 0}, {1, 0, 0, 0}}, 2 },  // 15x^4 * y^7
	{ {{-9, 0, 9, 0}}, 1 },                 // -9y^9
	{ {{21, 0, 7, 0}}, 1 },                 // 21y^7
	{ {{20, 8, 0, 0}}, 1 }                  // 20x^8
};
// B: -12x^4 * y^2 + 23x^4 + 3y^2 - 7
Teamgroup B3[] = {
	{ {{-12, 4, 2, 0}, {1, 0, 0, 0}}, 2 }, // -12x^4 * y^2
	{ {{23, 4, 0, 0}}, 1 },                 // 23x^4
	{ {{3, 0, 2, 0}}, 1 },                  // 3y^2
	{ {{-7, 0, 0, 0}}, 1 }                  // -7
};

// test case 4 (extreme2)
// A: 15x^4 * y^7 * 6z^3 - 9y^9 * z^2 + 21y^7 + 20x^8
Teamgroup A4[] = {
	{ {{15, 4, 7, 0}, {6, 0, 0, 3}}, 2 },  // 15x^4 * y^7 * 6z^3
	{ {{-9, 0, 9, 0}, {1, 0, 0, 2}}, 2 },  // -9y^9 * z^2
	{ {{21, 0, 7, 0}}, 1 },                 // 21y^7
	{ {{20, 8, 0, 0}}, 1 }                  // 20x^8
};
// B: -12x^4 * y^2 * z^2 + 23x^4 + 3y^2 - 7
Teamgroup B4[] = {
	{ {{-12, 4, 2, 0}, {1, 0, 0, 2}}, 2 }, // -12x^4 * y^2 * z^2
	{ {{23, 4, 0, 0}}, 1 },                 // 23x^4
	{ {{3, 0, 2, 0}}, 1 },                  // 3y^2
	{ {{-7, 0, 0, 0}}, 1 }                  // -7
};

polynomial terms[MAX_TERMS];

int compare_y(const void* a, const void* b) {
	polynomial* ta = (polynomial*)a;
	polynomial* tb = (polynomial*)b;
	return tb->yexp - ta->yexp;
}

void attach(polynomial* d)
{
	if (avail > MAX_TERMS) {
		fprintf(stderr, "Terms are so many\n");
		exit(1);
	}
	terms[avail].coef = d->coef;
	terms[avail].xexp = d->xexp;
	terms[avail].yexp = d->yexp;
	terms[avail].zexp = d->zexp;
	avail++;
}
int Preprocess(Teamgroup a[],int size) {
	int start = avail;
	polynomial result[size];
	for (int i = 0; i < size; i++)
	{
		Teamgroup cur = a[i];
		polynomial processing = { 1,0,0,0 };
		for (int j = 0; j < cur.factorsnum; j++)
		{
			processing.coef *= cur.factors[j].coef;
			processing.xexp += cur.factors[j].xexp;
			processing.yexp += cur.factors[j].yexp;
			processing.zexp += cur.factors[j].zexp;
		}
		result[i] = processing;
	}
	qsort(result,size,sizeof(polynomial),compare_y);
	for (int i = 0; i < size; i++)
	{
		attach(result + i);
	}
	return start;
}
void multPoly(int as, int ae, int be, int* cs, int* ce) {
	float tempcoef;
	polynomial add;
	*cs = avail;
	while (As <= Ae && Bs <= Be) {
		if (compare_y(terms[As],terms[Bs]) > 0) {
			attach(terms[as]); As++;
		}
		else if (compare_y(terms[As], terms[Bs]) == 0) {
			if (terms[As].xexp == terms[Bs].xexp && terms[As].zexp == terms[Bs].zexp) {
				add.coef = tempcoef;
			}
			tempcoef = terms[As].coef + terms[Bs].coef;
			if (tempcoef) {
				
			}
		}
	}
}

int main(void) {
	int As = Preprocess(A1, sizeof(A1) / sizeof(A1[0]));
	int Ae = avail - 1;
	int Bs = Preprocess(B1, sizeof(B1) / sizeof(B1[0]));
	int Be = avail - 1;
	int Cs, Ce;

	return 0;
}