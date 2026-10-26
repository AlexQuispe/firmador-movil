#!/usr/bin/env bash
# Jerarquía de laboratorio de I6 (sin validez legal). Ejecutar dentro de ca-laboratorio-i6/.
#
#   ./ca-i6.sh crear              raíz y CA emisora (una sola vez)
#   ./ca-i6.sh emitir titular.csr certificado del titular con el perfil de persona natural, cadena.pem y CRL
#   ./ca-i6.sh crl                vuelve a emitir las CRL de la raíz y de la CA
#   ./ca-i6.sh servir             publica raíz, CA y CRL en http://localhost:8099 (Ctrl+C para detener)
set -euo pipefail
cd "$(dirname "$0")"
CNF=openssl-i6.cnf
PUBLICADO=publicado

serie() { openssl rand -hex 8 | sed 's/^[89a-f]/1/'; } # 64 bits, positivo

crl() {
    mkdir -p "$PUBLICADO"
    openssl ca -config "$CNF" -name ca_raiz -gencrl -out raiz.crl.pem
    openssl ca -config "$CNF" -name ca_ca -gencrl -out ca.crl.pem
    openssl crl -in raiz.crl.pem -outform DER -out "$PUBLICADO/raiz.crl"
    openssl crl -in ca.crl.pem -outform DER -out "$PUBLICADO/ca.crl"
    cp raiz.pem ca.pem "$PUBLICADO/"
}

crear() {
    if [[ -e raiz.key ]]; then echo "La jerarquía ya existe; no se sobrescribe." >&2; exit 1; fi
    # Raíz autofirmada, RSA 4096, 10 años
    openssl req -x509 -new -newkey rsa:4096 -noenc -keyout raiz.key -out raiz.pem -days 3650 -sha256 \
        -config "$CNF" -section req_raiz -extensions ext_raiz -set_serial "0x$(serie)"
    # CA emisora, RSA 4096, 10 años, firmada por la raíz
    openssl req -new -newkey rsa:4096 -noenc -keyout ca.key -out ca.csr -config "$CNF" -section req_ca
    openssl x509 -req -in ca.csr -CA raiz.pem -CAkey raiz.key -set_serial "0x$(serie)" -days 3650 -sha256 \
        -extfile "$CNF" -extensions ext_ca -out ca.pem
    chmod 600 raiz.key ca.key
    : > raiz.index; : > ca.index; echo 01 > raiz.crlnumber; echo 01 > ca.crlnumber
    crl
    openssl verify -CAfile raiz.pem ca.pem
}

emitir() {
    local csr=$1
    openssl req -in "$csr" -noout -verify
    # Vigencia de un año, como el certificado de la ECA; el sujeto se copia tal cual de la CSR
    openssl x509 -req -in "$csr" -CA ca.pem -CAkey ca.key -set_serial "0x$(serie)" -days 365 -sha256 \
        -extfile "$CNF" -extensions ext_titular -out titular.pem
    cat titular.pem ca.pem raiz.pem > cadena.pem
    crl
    openssl verify -crl_check_all -CAfile raiz.pem -untrusted ca.pem \
        -CRLfile raiz.crl.pem -CRLfile ca.crl.pem titular.pem
}

case "${1:-}" in
    crear) crear ;;
    emitir) emitir "${2:?Falta la CSR}" ;;
    crl) crl ;;
    servir) exec python3 -I -m http.server 8099 --bind 127.0.0.1 --directory "$PUBLICADO" ;;
    *) sed -n '2,8p' "$0"; exit 1 ;;
esac
