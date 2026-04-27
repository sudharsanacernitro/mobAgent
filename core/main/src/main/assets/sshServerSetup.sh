#!/bin/sh

set -e

DEFAULT_PASS="mobAgent"

echo "[*] Stopping existing SSH (if running)..."
pkill sshd 2>/dev/null || true

echo "[*] Updating package index..."
apk update

echo "[*] Checking if OpenSSH is installed..."
if ! command -v sshd >/dev/null 2>&1; then
    echo "[*] Installing OpenSSH..."
    apk add openssh
else
    echo "[*] OpenSSH already installed, skipping..."
fi

echo "[*] Generating SSH host keys..."
ssh-keygen -A

echo "[*] Setting default root password..."
echo "root:$DEFAULT_PASS" | chpasswd

echo "[*] Configuring SSH..."
sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config

echo "[*] Starting SSH manually..."
/usr/sbin/sshd -p 8022

echo "[*] SSH Server started!"
echo "[*] Username: root"
echo "[*] Password: $DEFAULT_PASS"
echo "[*] Connect: ssh root@127.0.0.1"