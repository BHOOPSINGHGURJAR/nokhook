import React, { useMemo, useState } from 'react';
import {
  BarChart,
  Bar,
  CartesianGrid,
  Cell,
  PieChart,
  Pie,
  PolarAngleAxis,
  PolarGrid,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { BarChart3, PieChart as PieIcon, Radar as RadarIcon, Sparkles } from 'lucide-react';
import type { RatingStats, RatingTierKey, VibeSlice } from '../types';

export type EmotionalTag = 'Inspirational' | 'Dark' | 'Fast-paced' | 'Educational';

const EMOTIONAL_COLORS: Record<EmotionalTag, string> = {
  Inspirational: '#F59E0B',
  Dark: '#6366F1',
  'Fast-paced': '#EF4444',
  Educational: '#10B981',
};

export function computeRatingWeightedEmotionalVibes(
  baseVibes: VibeSlice[],
  ratingStats: RatingStats,
  userSelectedTier?: RatingTierKey | null
): Array<{ tag: EmotionalTag; percentage: number; estimatedVotes: number; color: string }> {
  const effective = { ...ratingStats };
  if (userSelectedTier === 'must_read') effective.mustReadVotes += 1;
  if (userSelectedTier === 'worth_reading') effective.worthReadingVotes += 1;
  if (userSelectedTier === 'light_read') effective.lightReadVotes += 1;
  if (userSelectedTier === 'skip') effective.skipVotes += 1;

  const totalVotes = Math.max(
    1,
    effective.mustReadVotes + effective.worthReadingVotes + effective.lightReadVotes + effective.skipVotes
  );

  const mustRatio = effective.mustReadVotes / totalVotes;
  const worthRatio = effective.worthReadingVotes / totalVotes;
  const lightRatio = effective.lightReadVotes / totalVotes;
  const skipRatio = effective.skipVotes / totalVotes;

  const scores: Record<EmotionalTag, number> = {
    Inspirational: 12 + mustRatio * 34,
    Dark: 10 + worthRatio * 20 + skipRatio * 18,
    'Fast-paced': 12 + lightRatio * 38 + mustRatio * 12,
    Educational: 10 + worthRatio * 28 + mustRatio * 14,
  };

  for (const slice of baseVibes) {
    const p = slice.percentage;
    const key = slice.name.toLowerCase();
    if (['suspense', 'mystery', 'satire'].includes(key)) {
      scores.Dark += p * 0.7;
      scores['Fast-paced'] += p * 0.55;
    } else if (key === 'adventure') {
      scores['Fast-paced'] += p * 0.78;
      scores.Inspirational += p * 0.35;
    } else if (['psychology', 'business', 'clarity', 'culture'].includes(key)) {
      scores.Educational += p * 0.8;
      scores.Inspirational += p * 0.45;
    } else if (['drama', 'nostalgia'].includes(key)) {
      scores.Inspirational += p * 0.55;
      scores.Dark += p * 0.4;
    } else {
      scores.Inspirational += p * 0.4;
      scores.Educational += p * 0.3;
    }
  }

  const sum = Object.values(scores).reduce((acc, v) => acc + v, 0) || 1;
  const sorted = (Object.entries(scores) as Array<[EmotionalTag, number]>).sort((a, b) => b[1] - a[1]);
  const rounded = sorted.map(([tag, score]) => ({
    tag,
    percentage: Math.max(4, Math.round((score / sum) * 100)),
  }));

  const diff = 100 - rounded.reduce((acc, item) => acc + item.percentage, 0);
  if (rounded.length > 0 && diff !== 0) {
    rounded[0].percentage = Math.max(4, rounded[0].percentage + diff);
  }

  return rounded.map((item) => ({
    ...item,
    estimatedVotes: Math.round((totalVotes * item.percentage) / 100),
    color: EMOTIONAL_COLORS[item.tag],
  }));
}

interface VibeChartRechartsProps {
  baseVibes: VibeSlice[];
  ratingStats: RatingStats;
  userSelectedTier?: RatingTierKey | null;
}

export const VibeChartRecharts: React.FC<VibeChartRechartsProps> = ({
  baseVibes,
  ratingStats,
  userSelectedTier,
}) => {
  const [mode, setMode] = useState<'bar' | 'donut' | 'radar'>('bar');
  const [selectedTag, setSelectedTag] = useState<EmotionalTag | null>(null);

  const data = useMemo(
    () => computeRatingWeightedEmotionalVibes(baseVibes, ratingStats, userSelectedTier),
    [baseVibes, ratingStats, userSelectedTier]
  );

  const activeItem = data.find((d) => d.tag === selectedTag) || data[0];
  const totalVotes =
    ratingStats.mustReadVotes +
    ratingStats.worthReadingVotes +
    ratingStats.lightReadVotes +
    ratingStats.skipVotes +
    (userSelectedTier ? 1 : 0);

  return (
    <div className="rounded-2xl border border-[#262634] bg-[#111117] p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-violet-400" />
            <h3 className="text-lg font-semibold text-white">Vibe Chart</h3>
          </div>
          <p className="text-xs text-slate-400">
            Emotional tags weighted from {totalVotes.toLocaleString()} reader ratings
          </p>
        </div>

        <div className="inline-flex rounded-xl border border-[#262634] bg-[#181822] p-1 gap-1">
          <button
            type="button"
            onClick={() => setMode('bar')}
            className={`px-2.5 py-1.5 rounded-lg text-xs font-medium inline-flex items-center gap-1.5 transition ${
              mode === 'bar' ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            <BarChart3 className="w-3.5 h-3.5" /> Bar
          </button>
          <button
            type="button"
            onClick={() => setMode('donut')}
            className={`px-2.5 py-1.5 rounded-lg text-xs font-medium inline-flex items-center gap-1.5 transition ${
              mode === 'donut' ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            <PieIcon className="w-3.5 h-3.5" /> Donut
          </button>
          <button
            type="button"
            onClick={() => setMode('radar')}
            className={`px-2.5 py-1.5 rounded-lg text-xs font-medium inline-flex items-center gap-1.5 transition ${
              mode === 'radar' ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            <RadarIcon className="w-3.5 h-3.5" /> Radar
          </button>
        </div>
      </div>

      {activeItem && (
        <div
          className="flex items-center justify-between rounded-xl border px-3.5 py-2.5 bg-[#09090D]/70"
          style={{ borderColor: `${activeItem.color}66` }}
        >
          <div className="flex items-center gap-2.5">
            <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: activeItem.color }} />
            <div>
              <p className="text-sm font-semibold text-white">{activeItem.tag}</p>
              <p className="text-xs text-slate-400">
                ~{activeItem.estimatedVotes.toLocaleString()} reader ratings tagged this vibe
              </p>
            </div>
          </div>
          <span
            className="px-2.5 py-1 rounded-full text-xs font-bold"
            style={{ backgroundColor: `${activeItem.color}26`, color: activeItem.color }}
          >
            {activeItem.percentage}%
          </span>
        </div>
      )}

      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          {mode === 'bar' ? (
            <BarChart data={data} margin={{ top: 10, right: 12, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#262634" vertical={false} />
              <XAxis dataKey="tag" stroke="#9CA3AF" fontSize={12} tickLine={false} />
              <YAxis stroke="#9CA3AF" fontSize={11} tickLine={false} unit="%" />
              <Tooltip
                cursor={{ fill: 'rgba(124, 58, 237, 0.08)' }}
                contentStyle={{
                  backgroundColor: '#181822',
                  borderColor: '#262634',
                  borderRadius: '12px',
                  color: '#F8FAFC',
                  fontSize: '12px',
                }}
                formatter={(value: number) => [`${value}%`, 'Vibe Share']}
              />
              <Bar
                dataKey="percentage"
                radius={[8, 8, 0, 0]}
                onClick={(entry) => setSelectedTag(entry.tag as EmotionalTag)}
              >
                {data.map((entry) => (
                  <Cell
                    key={entry.tag}
                    fill={entry.color}
                    fillOpacity={activeItem?.tag === entry.tag ? 1 : 0.72}
                    cursor="pointer"
                  />
                ))}
              </Bar>
            </BarChart>
          ) : mode === 'donut' ? (
            <PieChart>
              <Pie
                data={data}
                dataKey="percentage"
                nameKey="tag"
                cx="50%"
                cy="50%"
                innerRadius={56}
                outerRadius={88}
                paddingAngle={4}
                onClick={(entry) => setSelectedTag(entry.tag as EmotionalTag)}
              >
                {data.map((entry) => (
                  <Cell key={entry.tag} fill={entry.color} cursor="pointer" />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: '#181822',
                  borderColor: '#262634',
                  borderRadius: '12px',
                  color: '#F8FAFC',
                  fontSize: '12px',
                }}
                formatter={(value: number) => [`${value}%`, 'Vibe Share']}
              />
            </PieChart>
          ) : (
            <RadarChart data={data} cx="50%" cy="50%" outerRadius="72%">
              <PolarGrid stroke="#262634" />
              <PolarAngleAxis dataKey="tag" stroke="#9CA3AF" fontSize={12} />
              <Radar
                name="Vibe Share"
                dataKey="percentage"
                stroke="#8B5CF6"
                fill="#7C3AED"
                fillOpacity={0.38}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#181822',
                  borderColor: '#262634',
                  borderRadius: '12px',
                  color: '#F8FAFC',
                  fontSize: '12px',
                }}
                formatter={(value: number) => [`${value}%`, 'Vibe Share']}
              />
            </RadarChart>
          )}
        </ResponsiveContainer>
      </div>

      <div className="flex flex-wrap gap-2 justify-center">
        {data.map((item) => {
          const active = activeItem?.tag === item.tag;
          return (
            <button
              key={item.tag}
              type="button"
              onClick={() => setSelectedTag(item.tag)}
              className={`px-3 py-1.5 rounded-full text-xs border transition inline-flex items-center gap-1.5 ${
                active ? 'text-white font-semibold' : 'text-slate-300 border-[#262634] bg-[#09090D]'
              }`}
              style={
                active
                  ? { borderColor: item.color, backgroundColor: `${item.color}24` }
                  : undefined
              }
            >
              <span className="w-2 h-2 rounded-full" style={{ backgroundColor: item.color }} />
              {item.tag} ({item.percentage}%)
            </button>
          );
        })}
      </div>
    </div>
  );
};
