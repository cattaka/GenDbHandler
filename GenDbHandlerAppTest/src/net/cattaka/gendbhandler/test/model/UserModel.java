
package net.cattaka.gendbhandler.test.model;

import java.util.Date;
import java.util.List;

import net.cattaka.gendbhandler.test.model.coder.AuthorityCoder;
import net.cattaka.gendbhandler.test.model.coder.StringArrayCoder;
import net.cattaka.util.gendbhandler.Attribute;
import net.cattaka.util.gendbhandler.GenDbHandler;
import net.cattaka.util.gendbhandler.Attribute.FieldType;

@GenDbHandler(find = {
        "id", "username", "team:role+,id", "team:id-", ":id", "authority:id+"
}, unique = {
    "username"
})
public class UserModel {
    public enum Role {
        PROGRAMMER, DESIGNNER, MANAGER
    }

    public enum Authority {
        USER, ADMIN
    }

    @Attribute(primaryKey = true)
    private Long id;

    private String username;

    @Attribute(version = 2)
    private String nickname;

    @Attribute(version = 2)
    private String team;

    private Role role;

    private Date createdAt;

    @Attribute(customCoder = StringArrayCoder.class, customDataType = FieldType.BLOB)
    private List<String> tags;

    @Attribute(version = 3, customDataType = FieldType.INTEGER, customCoder = AuthorityCoder.class)
    private Authority authority;

    @Attribute(persistent = false)
    private Object userData;

    private byte[] blob;
    
    public UserModel() {
    }

    public UserModel(Long id, String username, String nickname, String team, Role role,
            Date createdAt, List<String> tags, Authority authority) {
        super();
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.team = team;
        this.role = role;
        this.createdAt = createdAt;
        this.tags = tags;
        this.authority = authority;
    }

    public UserModel(Long id, String username, String nickname, String team, Role role,
            Date createdAt, List<String> tags, Authority authority, byte[] blob) {
        this(id, username, nickname, team, role, createdAt, tags, authority);
        this.blob = blob;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public Authority getAuthority() {
        return authority;
    }

    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    public byte[] getBlob() {
        return blob;
    }

    public void setBlob(byte[] blob) {
        this.blob = blob;
    }
    
}
