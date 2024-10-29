# Shifting by a negative value is undefined behaviour

1. This Cppcheck error implementation with Error ID `shiftNegative` looks for instances where a bitwise shift operation is being performed by a negative value. In the provided source code snippet, the function `negativeBitwiseShiftError` is called with a parameter `op` representing the shift value, and if this value is negative, an error is reported.

2. This check is important for code quality and security because shifting by a negative value in C++ is considered undefined behavior. Undefined behavior can lead to unexpected program results, crashes, or security vulnerabilities. By detecting and flagging such occurrences, this check helps prevent potential bugs and ensures code reliability.

3. To fix the issue of shifting by a negative value and resolve the `shiftNegative` error reported by Cppcheck, you should review the logic that determines the shift value and ensure that it is not negative before performing the bitwise shift operation. This can be achieved by adding appropriate checks or validations in the code to handle negative shift values gracefully, such as by clamping the shift value to a non-negative range or adjusting the logic to avoid negative shifts altogether. By correcting the shift operation to use valid non-negative shift values, you can eliminate the risk of undefined behavior and improve the overall quality and safety of the code.

_(Text generated by Codacy Bot. Send feedback to ai-pattern-text@codacy.com)_