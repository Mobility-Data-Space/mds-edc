# Backup and Recovery Guide

This guide provides comprehensive backup and recovery procedures for the MDS EDC Connector deployment. Proper backup strategies are essential for:

- **Data protection** against hardware failures
- **Disaster recovery** capabilities
- **Compliance** requirements
- **Testing and development** environment setup

## Components Requiring Backup

The MDS EDC deployment has two critical data stores:

1. **PostgreSQL Database** - Stores connector operational data
2. **HashiCorp Vault** - Stores secrets and credentials

## PostgreSQL Backup and Recovery

### What PostgreSQL Contains

The PostgreSQL database stores:

- Asset definitions and data addresses
- Policy definitions (access control, usage policies)
- Contract definitions (asset-policy linkages)
- Contract agreements (negotiated contracts)
- Contract negotiations (negotiation history and state)
- Transfer processes (transfer history and state)
- Data plane instance information

**Loss Impact**: Without backups, losing PostgreSQL data means:

- All assets, policies, and contracts must be recreated
- Active transfers will fail
- Contract negotiation history is lost
- No recovery of business relationships

## Support

- [PostgreSQL Backup Documentation](https://www.postgresql.org/docs/current/backup.html)
- [Vault Backup Best Practices](https://developer.hashicorp.com/vault/tutorials/operations/backup)
- [Docker Volume Backup Strategies](https://docs.docker.com/storage/volumes/#backup-restore-or-migrate-data-volumes)
- [Disaster Recovery Planning Guide](https://www.ready.gov/business/implementation/IT)
