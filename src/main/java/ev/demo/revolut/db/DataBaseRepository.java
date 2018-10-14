package ev.demo.revolut.db;

import java.util.List;
import java.util.Optional;

public interface DataBaseRepository<T> {
    Optional<T> find(String id);
    List<T> findAll();
    T insert(T entity);
    T update(T entity);
    boolean delete(String id);
}
