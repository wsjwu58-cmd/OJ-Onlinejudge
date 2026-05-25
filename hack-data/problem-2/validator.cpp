#include <iostream>
using namespace std;
int main() {
    int a, b;
    if (!(cin >> a >> b)) return 1;              // 输入格式错误
    if (a < -1000 || a > 1000) return 2;         // 超出范围
    if (b < -1000 || b > 1000) return 3;
    char c;
    if (cin >> c) return 4;                      // 多余字符
    return 0;                                    // 合法
}