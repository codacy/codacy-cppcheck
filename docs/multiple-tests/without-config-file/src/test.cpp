#include <iostream>

void causeNullPointerError() {
    int* ptr = nullptr; // 1. Pointer is explicitly set to NULL
    
    // ... some other logic ...

    // 2. ERROR: Dereferencing the null pointer
    *ptr = 42; 
    
    std::cout << "Value: " << *ptr << std::endl;
}

void checkAfterDereference(int* x) {
    // 3. ERROR: Dereferencing 'x' before checking if it is null
    *x = 10; 
    
    if (x == nullptr) {
        return;
    }
}

int main() {
    causeNullPointerError();
    
    int* val = nullptr;
    checkAfterDereference(val);

    return 0;
}