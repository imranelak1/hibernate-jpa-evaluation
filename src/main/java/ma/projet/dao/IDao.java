package ma.projet.dao;
import java.io.Serializable;
import java.util.List;

public interface IDao<T,ID extends Serializable> {
    T create(T entity);
    T update(T entity);
    void delete(T entity);
    T findById(ID id);
    List<T> findAll();
}
