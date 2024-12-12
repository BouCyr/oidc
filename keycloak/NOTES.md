# TITLE

## Start kc

```shell


docker run --name mykeycloak -p 8021:8080 \
-e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=change_me --add-host host.docker.internal:host-gateway \
quay.io/keycloak/keycloak:latest \
start-dev

```

TODO : ajout d'un param 'host' en GET sur le endpoint de configuration pour surcharger le host (permettre
host.docker.internal pour permettre la com containerKC <> OIDC)

## realm/client

```json
{
"realm": "simple",
"auth-server-url": "http://localhost:8021/",
"ssl-required": "external",
"resource": "rawidc",
"credentials": {
"secret": "nPrsjoMNmHytfKzPGytSw1u3bFXFJwpt"
},
"confidential-port": 0
}
```