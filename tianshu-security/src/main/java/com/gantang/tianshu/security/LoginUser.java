package com.gantang.tianshu.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户主体。携带用户 ID、用户名、角色/权限及可选附加信息。
 *
 * @author gantang
 */
public class LoginUser implements UserDetails, CredentialsContainer {

    private String id;
    private String username;
    private transient String password;
    private Set<String> roles;
    private Map<String, Object> extra;
    private boolean enabled = true;

    public LoginUser() {
    }

    public LoginUser(String id, String username, String password, Collection<String> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.roles == null) {
            return List.of();
        }
        return this.roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    public Set<String> getRoles() {
        return this.roles;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public Map<String, Object> getExtra() {
        return this.extra;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
