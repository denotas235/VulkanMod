# VulkanMod - Sistema de Versionamento Local

## Estrutura Criada

```
~/
├── VulkanMod/                  # Diretório de desenvolvimento atual (working directory)
├── vulkanmod-releases/         # Histórico de versões (backups)
│   └── v0.1.0-initial/         # Primeira versão (backup de 2026-07-01)
├── vulkanmod-changelog.txt     # Histórico de mudanças
├── backup-version.sh           # Script para criar nova versão
├── restore-version.sh          # Script para restaurar versão anterior
└── list-versions.sh            # Script para listar versões disponíveis
```

## Comandos

### Criar nova versão (backup)
```bash
# Depois de terminar uma feature e testar:
./backup-version.sh v0.X.X-descricao

# Exemplos:
./backup-version.sh v0.1.1-surface-fix
./backup-version.sh v0.2.0-gpu-culling
./backup-version.sh v0.3.0-timeline-semaphores
```

O script:
- Valida o formato da versão (v0.X.X-descricao)
- Cria pasta em ~/vulkanmod-releases/
- Copia todos os arquivos (exclui .git, .gradle, build)
- Atualiza o changelog automaticamente
- Mostra estatísticas (arquivos, tamanho)

### Listar versões disponíveis
```bash
./list-versions.sh
```

Mostra:
- Todas as versões salvas
- Data de criação
- Número de arquivos
- Tamanho de cada versão
- Comando para restaurar

### Restaurar versão anterior
```bash
# Listar versões primeiro:
./list-versions.sh

# Restaurar:
./restore-version.sh v0.1.0-initial

# O script:
# 1. Pede confirmação
# 2. Cria backup de emergência do estado atual
# 3. Substitui todo o código em ~/VulkanMod
# 4. Preserva o .git se existir
```

### Restaurar backup de emergência
```bash
# Se algo der errado após restore:
./restore-version.sh emergency-backup
```

## Fluxo de Trabalho Recomendado

1. **Desenvolvimento**
   - Trabalhe em ~/VulkanMod normalmente
   - Faça mudanças, teste, compile

2. **Feature Completa**
   - Teste a feature no Minecraft
   - Verifique se está estável
   - Rode `./backup-version.sh v0.X.X-descricao`

3. **Nova Feature**
   - Continue desenvolvendo em ~/VulkanMod
   - Repita o processo

4. **Problema/Derrota**
   - Rode `./list-versions.sh`
   - Escolha versão estável conhecida
   - Rode `./restore-version.sh v0.X.X-estavel`

## Changelog

Edite `~/vulkanmod-changelog.txt` após cada versão para documentar:
- O que mudou
- Bugs corrigidos
- Features adicionadas
- Problemas conhecidos

## Dicas

- **Sempre teste** antes de criar versão
- **Descrições claras** no nome da versão (ex: v0.2.0-gpu-culling)
- **Mantenha changelog atualizado** - essencial para saber o que cada versão tem
- **Não apague** pastas em vulkanmod-releases/ - são seu histórico
- **Backup de emergência** é criado automaticamente antes de cada restore

## Recuperação de Desastres

Se algo der muito errado:

```bash
# 1. Liste backups de emergência
ls -la ~/vulkanmod-backup-emergency/

# 2. Restaure manualmente
cd ~/
rm -rf VulkanMod
cp -r vulkanmod-backup-emergency VulkanMod

# 3. Ou use o script
./restore-version.sh emergency-backup
```

## Próximas Versões (Exemplos)

- v0.1.1-surface-fix - Correção vkCreateAndroidSurfaceKHR
- v0.2.0-gpu-culling - GPU-driven culling implementado
- v0.2.1-timeline-sem - Timeline semaphores (VK_KHR_timeline_semaphore)
- v0.3.0-subpass-merge - Subpass merging para Mali-G52
- v0.3.1-float16 - Otimização float16
- v0.4.0-iris-compat - Compatibilidade completa com Iris shaders

================================================================================