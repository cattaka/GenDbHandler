package net.cattaka.gendbhandler.test.model;
import java.util.Date;
import java.util.List;
import net.cattaka.gendbhandler.test.model.coder.StringArrayCoder;
import net.cattaka.util.gendbhandler.Attribute;
import net.cattaka.util.gendbhandler.GenDbHandler;
import net.cattaka.util.gendbhandler.Attribute.FieldType;
@GenDbHandler(
		find={"id","username","team:role+,id","team:id-"},
		unique={"username"}
)
public class UserModel {
	public enum Role {
		PROGRAMMER,
		DESIGNNER,
		MANAGER
	}
	@Attribute(primaryKey=true)
	private Long id;
	private String username;
	@Attribute(version=2)
	private String nickname;
	@Attribute(version=2)
	private String team;
	@Attribute(version=3)
	private Role role;
	private Date createdAt;
	@Attribute(customCoder=StringArrayCoder.class, customDataType=FieldType.BLOB)
	private List<String> tags;
	
	@Attribute(persistent=false)
	private Object userData;
	
	public UserModel() {
	}
	public UserModel(Long id, String username, String nickname, String team,
			Role role, Date createdAt, List<String> tags) {
		super();
		this.id = id;
		this.username = username;
		this.nickname = nickname;
		this.team = team;
		this.role = role;
		this.createdAt = createdAt;
		this.tags = tags;
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
}