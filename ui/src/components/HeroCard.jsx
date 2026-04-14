import { motion } from 'framer-motion';

export default function HeroCard({ title, subtitle }) {
  return (
    <motion.div
      className="hero-card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <p className="eyebrow">Stepserve Marketplace</p>
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </motion.div>
  );
}
