@Service
public class DBService {

    private final JdbcTemplate jdbc;

    public PetService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean login(String username, String password) {
        return jdbc.queryForList("SELECT * FROM employees WHERE euser = ? AND epass = ?", username, password).size() > 0;
    }
}