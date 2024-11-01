# Address of local auto-variable assigned to a function parameter.

1. The `uselessAssignmentArg` Cppcheck pattern looks for instances in the code where a function parameter is assigned the address of a local auto-variable. This is considered a potential issue because local auto-variables are allocated on the stack and are freed when the function execution ends. Assigning the address of such a variable to a function parameter can lead to the parameter holding an invalid address once the function completes, potentially causing memory leaks or undefined behavior.

2. This check is important for code quality and security because assigning the address of a local auto-variable to a function parameter can introduce bugs and memory-related issues. If the function parameter is used outside the function scope, it may lead to accessing invalid memory locations or causing memory leaks. These issues can result in unpredictable behavior, crashes, or even security vulnerabilities if an attacker can exploit the incorrect memory access.

3. To fix this issue, you should avoid assigning the address of local auto-variables to function parameters if the intention is to use the parameter's value outside the function scope. Instead, consider passing the value directly as an argument, or if passing a reference is necessary, ensure that the referenced object has a valid lifetime beyond the function's execution. Alternatively, you can allocate memory dynamically (on the heap) for the variable and manage its lifetime appropriately to prevent issues related to invalid memory access.

_(Text generated by Codacy Bot. Send feedback to ai-pattern-text@codacy.com)_
