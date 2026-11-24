#!/usr/bin/env bash
# Kalıcı yükleme (upload) anahtarı oluşturur ve GitHub secret'larına girilecek değerleri yazar.
# Kullanım: scripts/create-release-keystore.sh [çıktı-dizini]
# Gerekli: JDK (keytool). Oluşan .jks dosyasını ve şifreleri güvenli bir yerde saklayın:
# kaybederseniz Play Console'dan yükleme anahtarı sıfırlama talep etmeniz gerekir.
set -euo pipefail

out="${1:-$HOME/teti-keystore}"
mkdir -p "$out"
chmod 700 "$out"
jks="$out/teti-upload.jks"
alias="teti-upload"

if [ -e "$jks" ]; then
  echo "Zaten var: $jks (üzerine yazılmadı)" >&2
  exit 1
fi

pass="$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)"

keytool -genkeypair -v \
  -keystore "$jks" -storetype PKCS12 \
  -alias "$alias" -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$pass" -keypass "$pass" \
  -dname "CN=TeTi, O=TeTi, C=TR"

base64 < "$jks" | tr -d '\n' > "$out/TETI_KEYSTORE_BASE64.txt"
umask 077
cat > "$out/github-secrets.txt" <<SECRETS
TETI_KEYSTORE_BASE64 = (TETI_KEYSTORE_BASE64.txt dosyasının içeriği)
TETI_KEYSTORE_PASSWORD = $pass
TETI_KEY_ALIAS = $alias
TETI_KEY_PASSWORD = $pass
SECRETS

echo
echo "Oluşturuldu: $jks"
echo "GitHub > Settings > Secrets and variables > Actions altına şunları ekleyin:"
echo "  $out/github-secrets.txt"
