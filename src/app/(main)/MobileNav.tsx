import { Column, Grid, Row, Text } from '@umami/react-zen';
import { WebsiteNav } from '@/app/(main)/websites/[websiteId]/WebsiteNav';
import { IconLabel } from '@/components/common/IconLabel';
import Link from '@/components/common/Link';
import { useMessages, useNavigation } from '@/components/hooks';
import { Globe, Grid2x2, LayoutDashboard, LinkIcon } from '@/components/icons';
import { MobileMenuButton } from '@/components/input/MobileMenuButton';
import { UserButton } from '@/components/input/UserButton';
import { Logo } from '@/components/svg';
import { AdminNav } from './admin/AdminNav';
import { SettingsNav } from './settings/SettingsNav';

export function MobileNav() {
  const { t, labels } = useMessages();
  const { pathname, websiteId, renderUrl } = useNavigation();
  const isAdmin = pathname.includes('/admin');
  const isSettings = pathname.includes('/settings');
  const isMain = !websiteId && !isAdmin && !isSettings;

  const links = [
    {
      id: 'boards',
      label: t(labels.boards),
      path: '/boards',
      icon: <LayoutDashboard />,
    },
    {
      id: 'websites',
      label: t(labels.websites),
      path: '/websites',
      icon: <Globe />,
    },
    {
      id: 'links',
      label: t(labels.links),
      path: '/links',
      icon: <LinkIcon />,
    },
    {
      id: 'pixels',
      label: t(labels.pixels),
      path: '/pixels',
      icon: <Grid2x2 />,
    },
  ];

  return (
    <Grid columns="auto 1fr" flexGrow={1} backgroundColor="surface-sunken" borderRadius>
      <MobileMenuButton>
        {({ close }) => {
          return (
            <Column gap="2" display="flex" flex-direction="column" padding="2" height="100%">
              {isMain &&
                links.map(link => {
                  const isActive = pathname.startsWith(link.path);
                  return (
                    <Row
                      key={link.id}
                      style={{
                        padding: '12px 14px',
                        borderRadius: '12px',
                        backgroundColor: isActive ? 'rgba(167, 139, 250, 0.14)' : 'transparent',
                        border: isActive ? '1px solid rgba(167, 139, 250, 0.28)' : '1px solid transparent',
                        transition: 'all 0.15s ease',
                        cursor: 'pointer',
                      }}
                    >
                      <Link
                        href={renderUrl(link.path)}
                        onClick={close}
                        style={{
                          width: '100%',
                          display: 'flex',
                          alignItems: 'center',
                          color: isActive ? '#ffffff' : 'rgba(255, 255, 255, 0.88)',
                          fontWeight: isActive ? 600 : 500,
                          fontSize: '15px',
                          textDecoration: 'none',
                        }}
                      >
                        <IconLabel icon={link.icon} label={link.label} />
                      </Link>
                    </Row>
                  );
                })}
              {websiteId && <WebsiteNav websiteId={websiteId} onItemClick={close} />}
              {isAdmin && <AdminNav onItemClick={close} />}
              {isSettings && <SettingsNav onItemClick={close} />}
              <Row style={{ marginTop: 'auto', paddingTop: '12px', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
                <UserButton onClose={close} />
              </Row>
            </Column>
          );
        }}
      </MobileMenuButton>
      <Row alignItems="center" justifyContent="center" flexGrow={1}>
        <IconLabel icon={<Logo />} style={{ width: 'auto' }}>
          <Text weight="bold">umami</Text>
        </IconLabel>
      </Row>
    </Grid>
  );
}
