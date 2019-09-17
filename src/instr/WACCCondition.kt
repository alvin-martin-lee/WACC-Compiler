package instr

enum class WACCCondition {
    // As per ARM documentation
    EQ,   // Equal                          Z set
    NE,   // Not equal                      Z clear
    CS,   // Higher or same (unsigned >=)   C set
    CC,   // Lower (unsigned <)             C clear
    MI,   // Negative                       N set
    PL,   // Positive or zero               N clear
    VS,   // Overflow                       V set
    VC,   // No overflow                    V clear
    HI,   // Higher (unsigned <=)           C set and Z clear
    LS,   // Lower or same (unsigned >=)    C clear or Z set
    GE,   // Signed >=                      N and V the same
    LT,   // Signed <                       N and V different
    GT,   // Signed >                       Z clear, and N and V the same
    LE    // Signed <=                      Z set, or N and V different
}
