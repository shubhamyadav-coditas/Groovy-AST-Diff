import java.util.*
import org.springframework.beans.factory.annotation.*
import com.company.model.*

class DataStore {
    def items = new ArrayList()
    def cache = new HashMap()
    
    @Autowired
    def repository
}
