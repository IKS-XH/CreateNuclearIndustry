/** 依Minecraft 1.21.1的帧尺寸、序列及20tick/s时间轴解析真实mcmeta；不补画缺帧。 */
export function timeline(width,height,metadata={}){
  const a=metadata.animation??{},w=a.width??(a.height?width:Math.min(width,height)),h=a.height??(a.width?height:Math.min(width,height));
  if(!Number.isInteger(w)||!Number.isInteger(h)||w<1||h<1||width%w||height%h)throw new Error('mcmeta帧尺寸不能整除PNG');
  const count=width/w*(height/h),defaultTime=a.frametime??1;
  const frames=(a.frames?.length?a.frames:Array.from({length:count},(_,i)=>i)).map(f=>({index:typeof f==='number'?f:f.index,ticks:typeof f==='number'?defaultTime:f.time??defaultTime}));
  for(const f of frames)if(!Number.isInteger(f.index)||f.index<0||f.index>=count||!Number.isInteger(f.ticks)||f.ticks<1)throw new Error('mcmeta帧索引或持续时间无效');
  return {width,height,frameWidth:w,frameHeight:h,columns:width/w,count,frames,interpolate:!!a.interpolate,totalTicks:frames.reduce((s,f)=>s+f.ticks,0)};
}
export function sample(t,ms){
  let tick=((ms/50)%t.totalTicks+t.totalTicks)%t.totalTicks,ordinal=0;
  while(tick>=t.frames[ordinal].ticks){tick-=t.frames[ordinal].ticks;ordinal++;}
  return {ordinal,index:t.frames[ordinal].index,next:t.frames[(ordinal+1)%t.frames.length].index,blend:t.interpolate?tick/t.frames[ordinal].ticks:0,elapsedTicks:tick};
}
export function nextFrameMs(t,ms){const cycle=Math.floor(ms/(t.totalTicks*50)),s=sample(t,ms),before=t.frames.slice(0,s.ordinal+1).reduce((n,f)=>n+f.ticks,0);return cycle*t.totalTicks*50+before*50;}
