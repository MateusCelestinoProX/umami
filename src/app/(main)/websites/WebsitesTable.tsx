import { DataColumn, DataTable, type DataTableProps, Icon, Text } from '@umami/react-zen';
import { type ReactNode, useMemo } from 'react';
import { DateDistance } from '@/components/common/DateDistance';
import { Favicon } from '@/components/common/Favicon';
import Link from '@/components/common/Link';
import { LinkButton } from '@/components/common/LinkButton';
import { SortableLabel } from '@/components/common/SortableLabel';
import { useMessages, useNavigation, useWebsiteListChartsQuery } from '@/components/hooks';
import { SquarePen } from '@/components/icons';
import { decodePunycodeDomain } from '@/lib/format';
import { WebsiteSparkline } from './WebsiteSparkline';

export interface WebsitesTableProps extends DataTableProps {
  showActions?: boolean;
  renderLink?: (row: any) => ReactNode;
}

export function WebsitesTable({
  showActions,
  renderLink,
  data = [],
  ...props
}: WebsitesTableProps & { data?: any[] }) {
  const { t, labels } = useMessages();
  const { renderUrl } = useNavigation();
  const websiteIds = useMemo(() => data.map(row => row.id), [data]);
  const chartsQuery = useWebsiteListChartsQuery(websiteIds);
  const charts = chartsQuery.data?.data || {};
  const isChartLoading = chartsQuery.isLoading && !chartsQuery.data;

  const isCards = (props as any).displayMode === 'cards';

  if (isCards) {
    return (
      <div
        className="websites-cards-list"
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          width: '100%',
        }}
      >
        {data.map((row: any) => {
          const chart = charts[row.id];
          const totalVisitors = chart?.total ?? 0;
          const editUrl = renderUrl(`/websites/${row.id}/settings`);

          return (
            <div
              key={row.id}
              className="glass-website-card"
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
                padding: '16px',
                borderRadius: '16px',
                background: 'linear-gradient(165deg, rgba(22, 22, 36, 0.76) 0%, rgba(12, 12, 22, 0.85) 100%)',
                backdropFilter: 'blur(16px)',
                WebkitBackdropFilter: 'blur(16px)',
                border: '1px solid rgba(255, 255, 255, 0.09)',
                boxShadow: '0 4px 16px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.12)',
                position: 'relative',
                overflow: 'hidden',
                contain: 'paint',
              }}
            >
              {/* Header: Favicon + Website Title (left) & Edit button (right) */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '10px',
                  width: '100%',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', minWidth: 0, flex: 1 }}>
                  {renderLink ? (
                    renderLink(row)
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0, flex: 1 }}>
                      <div style={{ flexShrink: 0, width: 22, height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Favicon domain={row.domain} />
                      </div>
                      <Text truncate title={row.name} style={{ maxWidth: '100%', fontWeight: 600, fontSize: '15px', color: '#ffffff' }}>
                        <Link href={renderUrl(`/websites/${row.id}`, false)}>{row.name}</Link>
                      </Text>
                    </div>
                  )}
                </div>
                {showActions && (
                  <LinkButton
                    href={editUrl}
                    variant="quiet"
                    style={{
                      flexShrink: 0,
                      minWidth: '44px',
                      minHeight: '44px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderRadius: '10px',
                    }}
                  >
                    <Icon size="sm">
                      <SquarePen />
                    </Icon>
                  </LinkButton>
                )}
              </div>

              {/* Domain Badge */}
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <span
                  style={{
                    display: 'inline-block',
                    fontSize: '12px',
                    fontFamily: 'monospace',
                    color: '#c4b5fd',
                    background: 'rgba(167, 139, 250, 0.08)',
                    border: '1px solid rgba(167, 139, 250, 0.22)',
                    padding: '3px 10px',
                    borderRadius: '8px',
                    maxWidth: '100%',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    fontWeight: 500,
                  }}
                  title={decodePunycodeDomain(row.domain) ?? undefined}
                >
                  {decodePunycodeDomain(row.domain)}
                </span>
              </div>

              {/* Footer: Created date distance on left, visitors count + Sparkline on right */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  paddingTop: '10px',
                  borderTop: '1px solid rgba(255, 255, 255, 0.06)',
                  marginTop: '2px',
                }}
              >
                <div style={{ fontSize: '12px', color: 'rgba(255, 255, 255, 0.55)' }}>
                  <DateDistance date={new Date(row.createdAt)} />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span
                    style={{
                      fontSize: '12px',
                      fontWeight: 600,
                      color: 'rgba(255, 255, 255, 0.90)',
                    }}
                  >
                    {totalVisitors.toLocaleString()} {t(labels.visitors).toLocaleLowerCase()}
                  </span>
                  <div style={{ width: 68, height: 22 }}>
                    <WebsiteSparkline
                      values={chart?.values}
                      total={chart?.total}
                      isLoading={isChartLoading}
                    />
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  return (
    <DataTable {...props} data={data}>
      <DataColumn
        id="name"
        label={<SortableLabel label={t(labels.name)} sortKey="name" />}
        style={{ minWidth: 0 }}
      >
        {renderLink}
      </DataColumn>
      <DataColumn
        id="domain"
        label={<SortableLabel label={t(labels.domain)} sortKey="domain" />}
        style={{ minWidth: 0 }}
      >
        {(row: any) => (
          <Text
            truncate
            title={decodePunycodeDomain(row.domain) ?? undefined}
            style={{ maxWidth: '100%' }}
          >
            {decodePunycodeDomain(row.domain)}
          </Text>
        )}
      </DataColumn>
      <DataColumn
        id="chart"
        label={<span style={{ whiteSpace: 'normal' }}>{`${t(labels.visitors)} (7d)`}</span>}
        style={{ minWidth: 0 }}
      >
        {(row: any) => {
          const chart = charts[row.id];

          return (
            <WebsiteSparkline
              values={chart?.values}
              total={chart?.total}
              isLoading={isChartLoading}
            />
          );
        }}
      </DataColumn>
      <DataColumn
        id="created"
        label={
          <SortableLabel label={t(labels.created)} sortKey="createdAt" defaultDirection="desc" />
        }
        width="180px"
      >
        {(row: any) => <DateDistance date={new Date(row.createdAt)} />}
      </DataColumn>
      {showActions && (
        <DataColumn id="action" label=" " align="end" width="48px">
          {(row: any) => {
            const websiteId = row.id;

            return (
              <LinkButton href={renderUrl(`/websites/${websiteId}/settings`)} variant="quiet">
                <Icon>
                  <SquarePen />
                </Icon>
              </LinkButton>
            );
          }}
        </DataColumn>
      )}
    </DataTable>
  );
}
