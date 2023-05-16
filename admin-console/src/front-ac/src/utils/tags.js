export const getTagColor = (colors, tag) => {
  if (!tag || !colors || tag.length === 0) return ;
  let hash = 0;
  for (let i = 0; i < tag.length; i++) {
    hash = ((hash << 5) - hash) + tag.charCodeAt(i);
    hash |= 0;
  }
  hash &= 0xfffffff;
  hash %= colors.length;
  return colors[hash];
}