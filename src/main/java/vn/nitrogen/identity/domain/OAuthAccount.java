package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

@Entity
@Table(schema = "identity", name = "oauth_accounts")
public class OAuthAccount extends AbstractIdentifiableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected OAuthAccount(){

    }
    private OAuthAccount(User user, String provider, String providerSubject, String providerEmail){
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public void setProviderSubject(String providerSubject) {
        this.providerSubject = providerSubject;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(long rowVersion) {
        this.rowVersion = rowVersion;
    }

    public static OAuthAccount link(
            User user,
            String provider,
            String providerSubject,
            String providerEmail
    ){
        return new OAuthAccount(user, provider, providerSubject, providerEmail);
    }

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        linkedAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void onUpdate(){
        updatedAt = Instant.now();
    }
}
