package com.dbfinanceira.appb.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fonte de chaves publicas (JWKS) lida de um ARQUIVO LOCAL, em vez de buscada via rede
 * no endpoint do Keycloak (.../protocol/openid-connect/certs) a cada validacao.
 *
 * Motivo tipico em instituicao financeira: a App B roda em um segmento de rede sem
 * saida direta para o Keycloak; o JWKS e sincronizado para essa pasta por um processo
 * separado (ver scripts/sync-jwks.sh) e a aplicacao apenas le o arquivo.
 *
 * O arquivo e recarregado automaticamente quando o "last modified time" muda -
 * nao e necessario reiniciar a App B apos uma rotacao de chaves no Keycloak,
 * desde que o processo de sincronizacao atualize o arquivo a tempo.
 *
 * Risco operacional a monitorar: se o arquivo local ficar "stale" (processo de sync
 * parado/falhando) e o Keycloak rotacionar a chave de assinatura, tokens novos
 * (kid desconhecido) comecam a ser rejeitados por esta aplicacao. Ver getLastLoadedAt().
 */
public class LocalFileJWKSource implements JWKSource<SecurityContext> {

    private static final Logger log = LoggerFactory.getLogger(LocalFileJWKSource.class);

    private final Path jwksPath;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile JWKSet cachedJwkSet;
    private volatile FileTime lastFileModifiedTime;
    private volatile Instant lastLoadedAt = Instant.EPOCH;

    public LocalFileJWKSource(Path jwksPath) {
        this.jwksPath = jwksPath;
        reload(); // falha rapido na subida se o arquivo nao existir ou for invalido
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, SecurityContext context) {
        refreshIfStale();
        return jwkSelector.select(cachedJwkSet);
    }

    /** Exposto para health checks/metrics: ha quanto tempo o JWKS local foi carregado. */
    public Instant getLastLoadedAt() {
        return lastLoadedAt;
    }

    private void refreshIfStale() {
        try {
            FileTime current = Files.getLastModifiedTime(jwksPath);
            if (!current.equals(lastFileModifiedTime)) {
                lock.lock();
                try {
                    FileTime recheck = Files.getLastModifiedTime(jwksPath);
                    if (!recheck.equals(lastFileModifiedTime)) {
                        reload();
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            log.warn("Nao foi possivel checar atualizacao do JWKS local em {}. Mantendo ultima versao valida (carregada em {}).",
                    jwksPath, lastLoadedAt, e);
        }
    }

    private void reload() {
        try {
            JWKSet parsed = JWKSet.load(jwksPath.toFile());
            this.cachedJwkSet = parsed;
            this.lastFileModifiedTime = Files.getLastModifiedTime(jwksPath);
            this.lastLoadedAt = Instant.now();
            log.info("JWKS local recarregado de {} ({} chave(s)).", jwksPath, parsed.getKeys().size());
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("Falha ao carregar JWKS local em " + jwksPath, e);
        }
    }
}
