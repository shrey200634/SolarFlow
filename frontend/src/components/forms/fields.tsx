import { forwardRef, useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'

const control =
  'h-9 w-full rounded-md border bg-surface px-3 text-sm placeholder:text-muted/70 focus:outline-2 focus:outline-offset-0 focus:outline-brand disabled:bg-canvas disabled:text-muted'

function FieldShell({ id, label, error, hint, children }: { id: string; label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium">{label}</label>
      {children}
      {error ? (
        <p id={`${id}-msg`} className="mt-1 text-xs text-danger">{error}</p>
      ) : hint ? (
        <p id={`${id}-msg`} className="mt-1 text-xs text-muted">{hint}</p>
      ) : null}
    </div>
  )
}

interface TextProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
}

export const TextField = forwardRef<HTMLInputElement, TextProps>(function TextField({ label, error, hint, id, className = '', ...rest }, ref) {
  const auto = useId()
  const fid = id ?? auto
  return (
    <FieldShell id={fid} label={label} error={error} hint={hint}>
      <input
        ref={ref}
        id={fid}
        aria-invalid={!!error}
        aria-describedby={error || hint ? `${fid}-msg` : undefined}
        className={`${control} ${error ? 'border-danger' : 'border-line'} ${className}`}
        {...rest}
      />
    </FieldShell>
  )
})

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  hint?: string
}

export const SelectField = forwardRef<HTMLSelectElement, SelectProps>(function SelectField({ label, error, hint, id, className = '', children, ...rest }, ref) {
  const auto = useId()
  const fid = id ?? auto
  return (
    <FieldShell id={fid} label={label} error={error} hint={hint}>
      <select
        ref={ref}
        id={fid}
        aria-invalid={!!error}
        aria-describedby={error || hint ? `${fid}-msg` : undefined}
        className={`${control} ${error ? 'border-danger' : 'border-line'} ${className}`}
        {...rest}
      >
        {children}
      </select>
    </FieldShell>
  )
})

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: ReactNode
  error?: string
}

export const CheckboxField = forwardRef<HTMLInputElement, CheckboxProps>(function CheckboxField({ label, error, id, ...rest }, ref) {
  const auto = useId()
  const fid = id ?? auto
  return (
    <div>
      <div className="flex items-start gap-2">
        <input ref={ref} id={fid} type="checkbox" className="mt-0.5 h-4 w-4 rounded border-line accent-brand" {...rest} />
        <label htmlFor={fid} className="text-sm">{label}</label>
      </div>
      {error && <p className="mt-1 text-xs text-danger">{error}</p>}
    </div>
  )
})