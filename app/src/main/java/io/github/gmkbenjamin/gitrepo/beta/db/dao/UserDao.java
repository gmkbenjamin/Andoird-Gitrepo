package io.github.gmkbenjamin.gitrepo.beta.db.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.db.DBC;
import io.github.gmkbenjamin.gitrepo.beta.db.DBHelper;
import io.github.gmkbenjamin.gitrepo.beta.db.entity.Permission;
import io.github.gmkbenjamin.gitrepo.beta.db.entity.User;

public class UserDao extends BaseDao<DBHelper, User, Integer> {

    public UserDao(DBHelper dbHelper, Dao<User, Integer> dao) {
        super(dbHelper, dao);
    }

    /** Atomic legacy-hash migration; does not overwrite a reset or disabled account. */
    public int upgradePasswordIfUnchanged(int id, String previous, String replacement) throws SQLException {
        UpdateBuilder<User, Integer> update = dao.updateBuilder();
        update.updateColumnValue(DBC.users.column_password, replacement);
        update.where().eq(DBC.users.column_id, id).and()
                .eq(DBC.users.column_password, new SelectArg(previous)).and()
                .eq(DBC.users.column_active, true);
        return update.update();
    }

    public User queryForUsername(String username) throws SQLException {
        SelectArg usernameArg = new SelectArg(username);

        List<User> users = dao.queryBuilder().where().eq(DBC.users.column_username, usernameArg).query();

        if (users.size() > 0) {
            return users.get(0);
        }

        return null;
    }

    public User queryForEmail(String email) throws SQLException {
        SelectArg usernameArg = new SelectArg(email);

        List<User> users = dao.queryBuilder().where().eq(DBC.users.column_email, usernameArg).query();

        if (users.size() > 0) {
            return users.get(0);
        }

        return null;
    }

    public User queryForUsernameAndActive(String username) throws SQLException {
        SelectArg usernameArg = new SelectArg(username);

        List<User> users = dao.queryBuilder().where().eq(DBC.users.column_active, true).and().eq(DBC.users.column_username, usernameArg).query();

        if (users.size() > 0) {
            return users.get(0);
        }

        return null;
    }

    public User queryForPublickey(String publickey) throws SQLException {
        SelectArg publickeyArg = new SelectArg(publickey);

        List<User> users = dao.queryBuilder().where().eq(DBC.users.column_publickey, publickeyArg).query();

        if (users.size() > 0) {
            return users.get(0);
        }

        return null;
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        dbHelper.getPermissionDao().deleteByUserId(id);

        return super.deleteById(id);
    }

    public List<User> getAllUsersWithoutPermissionForRepositoryId(int repositoryId) throws SQLException {
        List<User> users = queryForAll();
        List<Permission> permissions = dbHelper.getPermissionDao().getAllByRepositoryId(repositoryId);

        for (Permission permission : permissions) {
            Iterator<User> iter = users.iterator();
            while (iter.hasNext()) {
                User user = iter.next();
                if (user.getId() == permission.getUser().getId()) {
                    users.remove(user);
                    break;
                }
            }
        }

        return users;
    }
}
