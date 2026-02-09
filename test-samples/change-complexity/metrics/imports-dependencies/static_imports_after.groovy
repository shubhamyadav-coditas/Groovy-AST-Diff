import static java.lang.Math.PI
import static java.lang.Math.abs
import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.trim

class MathHelper {
    
    double calculate(double value) {
        return abs(value) * PI
    }
    
    String process(String input) {
        if (isBlank(input)) {
            return ""
        }
        return trim(input)
    }
}
