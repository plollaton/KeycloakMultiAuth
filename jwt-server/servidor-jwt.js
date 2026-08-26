#!/usr/bin/env node

/**
 * Servidor local que gera JWT com RS256
 *
 * Instalação:
 * npm install express jsonwebtoken
 *
 * Uso:
 * node servidor-jwt.js
 *
 * Depois, no Postman, configure um pre-request script que chama este servidor
 */

const express = require('express');
const jwt = require('jsonwebtoken');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 3001;



// =====================================================
// ROTAS
// =====================================================

/**
 * GET /jwt
 * Retorna um novo JWT válido por 60 segundos
 *
 * Uso:
 * curl http://localhost:3001/jwt
 *
 * Response:
 * {
 *   "jwt": "eyJhbGc...",
 *   "exp": 1691234567,
 *   "iat": 1691234507
 * }
 */
app.get('/jwt', (req, res) => {
    try {
        // =====================================================
        // CONFIGURAÇÃO
        // =====================================================

        const PRIVATE_KEY_PATH = './of-consentimentos.pem';

        // Validar arquivo
        if (!fs.existsSync(PRIVATE_KEY_PATH)) {
            console.error(`❌ Erro: Arquivo não encontrado: ${PRIVATE_KEY_PATH}`);
            process.exit(1);
        }

        const privateKey = fs.readFileSync(PRIVATE_KEY_PATH, 'utf8');
        const now = Math.floor(Date.now() / 1000);
        const exp = now + 60;

        const payload = {
            "jti": `novaAppA-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
            "iss": "of-consentimentos",
            "sub": "of-consentimentos",
            "client_id": "of-pagamentos",
            "aud": "http://localhost:8080/realms/master",
            "iat": now,
            "exp": exp
        };

        const token = jwt.sign(payload, privateKey, {
            algorithm: 'RS256',
            header: {
                alg: 'RS256',
                typ: 'JWT'
            }
        });

        res.json({
            jwt: token,
            iat: now,
            exp: exp,
            expires_in: 60
        });

    } catch (error) {
        console.error('Erro ao gerar JWT:', error.message);
        res.status(500).json({
            error: 'Erro ao gerar JWT',
            message: error.message
        });
    }
});

app.get('/jwt-pagamentos', (req, res) => {
    try {
        // =====================================================
        // CONFIGURAÇÃO
        // =====================================================

        const PRIVATE_KEY_PATH = './servico-a-private.pem';

        // Validar arquivo
        if (!fs.existsSync(PRIVATE_KEY_PATH)) {
            console.error(`❌ Erro: Arquivo não encontrado: ${PRIVATE_KEY_PATH}`);
            process.exit(1);
        }

        const privateKey = fs.readFileSync(PRIVATE_KEY_PATH, 'utf8');
        const now = Math.floor(Date.now() / 1000);
        const exp = now + 60;

        const payload = {
            "jti": `seervico-a-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
            "iss": "servico-a",
            "sub": "servico-a",
            "aud": "http://localhost:8080/realms/master",
            "iat": now,
            "exp": exp
        };

        const token = jwt.sign(payload, privateKey, {
            algorithm: 'RS256',
            header: {
                alg: 'RS256',
                typ: 'JWT'
            }
        });

        res.json({
            jwt: token,
            iat: now,
            exp: exp,
            expires_in: 60
        });

    } catch (error) {
        console.error('Erro ao gerar JWT:', error.message);
        res.status(500).json({
            error: 'Erro ao gerar JWT',
            message: error.message
        });
    }
});

/**
 * GET /health
 * Verifica se o servidor está rodando
 */
app.get('/health', (req, res) => {
    res.json({ status: 'ok', server: 'jwt-generator' });
});

// =====================================================
// INICIAR SERVIDOR
// =====================================================

app.listen(PORT, () => {
    console.log('\n✅ Servidor JWT iniciado!');
    console.log(`📍 URL: http://localhost:${PORT}`);
    console.log(`\n📝 Endpoints disponíveis:`);
    console.log(`  GET /jwt     → Gera novo JWT RS256`);    
    console.log(`  GET /jwt-pagamentos  → Gera novo JWT RS256 para pagamentos`);
    console.log(`  GET /health  → Status do servidor`);
    console.log(`\n💡 Teste no navegador: http://localhost:${PORT}/jwt`);
    console.log(`\n⏹️  Para parar: Ctrl+C\n`);
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n\n👋 Servidor parado');
    process.exit(0);
});