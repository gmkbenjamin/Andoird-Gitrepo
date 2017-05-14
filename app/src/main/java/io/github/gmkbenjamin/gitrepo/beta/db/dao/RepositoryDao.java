package io.github.gmkbenjamin.gitrepo.beta.db.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.SelectArg;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.db.DBC;
import io.github.gmkbenjamin.gitrepo.beta.db.DBHelper;
import io.github.gmkbenjamin.gitrepo.beta.db.entity.Permission;
import io.github.gmkbenjamin.gitrepo.beta.db.entity.Repository;

public class RepositoryDao extends BaseDao<DBHelper, Repository, Integer> {

    public RepositoryDao(DBHelper dbHelper, Dao<Repository, Integer> dao) {
        super(dbHelper, dao);
    }

    public Repository queryForMapping(String mapping) throws SQLException {
        SelectArg usernameArg = new SelectArg(mapping);

        List<Repository> repositories = dao.queryBuilder().where().eq(DBC.repositories.column_mapping, usernameArg).query();

        if (repositories.size() > 0) {
            return repositories.get(0);
        }

        return null;
    }

    public Repository queryForMappingAndActive(String mapping) throws SQLException {
        SelectArg usernameArg = new SelectArg(mapping);

        List<Repository> repositories = dao.queryBuilder().where().eq(DBC.repositories.column_mapping, usernameArg).and().eq(DBC.repositories.column_active, true).query();

        if (repositories.size() > 0) {
            return repositories.get(0);
        }

        return null;
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        dbHelper.getPermissionDao().deleteByRepositoryId(id);

        return super.deleteById(id);
    }

    public List<Repository> getAllRepositoriesWithoutPermissionForUserId(int userId) throws SQLException {
        List<Repository> repositories = queryForAll();
        List<Permission> permissions = dbHelper.getPermissionDao().getAllByUserId(userId);

        for (Permission permission : permissions) {
            Iterator<Repository> iter = repositories.iterator();
            while (iter.hasNext()) {
                Repository repository = iter.next();
                if (repository.getId() == permission.getRepository().getId()) {
                    repositories.remove(repository);
                    break;
                }
            }
        }

        return repositories;
    }
}
