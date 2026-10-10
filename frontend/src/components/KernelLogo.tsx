type KernelLogoProps = {
  className?: string
  alt?: string
}

/** Shared use of the official symbol-only Kernel mark. */
export default function KernelLogo({ className, alt = 'MIT Tech Kernel symbol' }: KernelLogoProps) {
  return <img className={className} src="/assets/kernel-mark.png" alt={alt} />
}
